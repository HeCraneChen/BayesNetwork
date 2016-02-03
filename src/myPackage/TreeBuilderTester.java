package myPackage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import myPackage.Tree.Line;
import myPackage.Tree.UnDirectionLine;
import myPackage.DataSet.Attribute;
import myPackage.Tree.UnDirectionLineComparetor;

public class TreeBuilderTester {
    DataSet trainDataSet;

    DataSet testDataSet;

    Tree tree;

    BufferedWriter br;

    public TreeBuilderTester(DataSet trainDataSet, DataSet testDataSet, BufferedWriter br) {
        this.trainDataSet = trainDataSet;
        this.testDataSet = testDataSet;
        this.br = br;
    }

    public void buildNaiveBayes() {
        tree = new Tree();

        // build tree.
        for(int i=0; i<trainDataSet.attributeList.size(); i++) {
            Line line = new Line(trainDataSet.classLabel, trainDataSet.attributeList.get(i));
            tree.lines.add(line);
        }
    }

    public void buildTAN() throws IOException {
        tree = new Tree();

        // build tree
        buildMST();
        assignDirections();
        for(int i=0; i<trainDataSet.attributeList.size(); i++) {
            Line line = new Line(trainDataSet.classLabel, trainDataSet.attributeList.get(i));
            tree.lines.add(line);
        }
    }

    public void outputModel() throws IOException {
        // Output.
        for(int i=0; i<trainDataSet.attributeList.size(); i++) {
            Attribute attribute = trainDataSet.attributeList.get(i);
            ArrayList<Attribute> parent = new ArrayList<>();

            // Get all parents.
            for(Line line : tree.lines) {
                if(line.endPoint.equals(attribute)) parent.add(line.startPoint);
            }

            // Sort.
            Collections.sort(parent, new Comparator<Attribute>() {
                @Override
                public int compare(Attribute o1, Attribute o2) {
                    if (o1.equals(trainDataSet.classLabel)) return 1;
                    if (o2.equals(trainDataSet.classLabel)) return -1;

                    if (trainDataSet.attributeList.indexOf(o1) > trainDataSet.attributeList.indexOf(o2)) return 1;
                    else return -1;
                }
            });

            // Output.
            br.write(attribute.name);
            for (Attribute aParent : parent) br.write(" " + aParent.name);
            br.newLine();

        }
        br.newLine();
    }

    private void buildMST() {
        ArrayList<Attribute> vnew = new ArrayList<>();
        ArrayList<Attribute> v = trainDataSet.attributeList;

        ArrayList<UnDirectionLine> enew = new ArrayList<>();
        ArrayList<UnDirectionLine> e = new ArrayList<>();

        // Init.
        vnew.add(trainDataSet.attributeList.get(0));
        for(Attribute X1 : v) {
            for(Attribute X2 : v) {
                UnDirectionLine line = new UnDirectionLine();
                line.point1 = X1;
                line.point2 = X2;
                line.i = trainDataSet.i(X1, X2);
                e.add(line);
            }
        }
        Collections.sort(e, new UnDirectionLineComparetor(trainDataSet));

        // Loop
        while(vnew.size() < trainDataSet.attributeList.size()) {
            for(int i=e.size() -1; i >= 0; i--) {
                if(vnew.contains(e.get(i).point1) && !vnew.contains(e.get(i).point2)) {
                    vnew.add(e.get(i).point2);
                    enew.add(e.get(i));
                    break;
                }
                if(!vnew.contains(e.get(i).point1) && vnew.contains(e.get(i).point2)) {
                    vnew.add(e.get(i).point1);
                    enew.add(e.get(i));
                    break;
                }
            }
        }

        // finally.
        tree.unDirectionLines = enew;
    }

    private void assignDirections() {
        Attribute root = trainDataSet.attributeList.get(0);

        // Add first.
        ArrayList<Attribute> que = new ArrayList<>();
        que.add(root);

        // Loop.
        while(!que.isEmpty()) {
            Attribute current = que.get(0);
            que.remove(0);

            for(int i = tree.unDirectionLines.size() -1; i >= 0; i--){
                if(tree.unDirectionLines.get(i).point1.equals(current)) {
                    Line line = new Line(current, tree.unDirectionLines.get(i).point2);
                    tree.lines.add(line);
                    que.add(line.endPoint);
                    tree.unDirectionLines.remove(i);
                }
                else if(tree.unDirectionLines.get(i).point2.equals(current)) {
                    Line line = new Line(current, tree.unDirectionLines.get(i).point1);
                    tree.lines.add(line);
                    que.add(line.endPoint);
                    tree.unDirectionLines.remove(i);
                }
            }
        }
    }

    public void test() throws IOException {
        int correct = 0;

        for(int i=0; i<testDataSet.data.size(); i++) {
            Map<Attribute, String> record = testDataSet.data.get(i);

            // Find the max
            double res = -1;
            String resString = "";
            for(String yString : testDataSet.classLabel.possibleValues) {
                double tmpRes = trainDataSet.pyX(yString, record, tree);

                if(tmpRes > res) {
                    res = tmpRes;
                    resString = yString;
                }
            }

            // Output.
            br.write(resString + " " + record.get(testDataSet.classLabel) + " ");
            br.write(Double.toString(res));
            br.newLine();

            if(record.get(testDataSet.classLabel).equals(resString)) correct++;
        }

        br.newLine();
        br.write(Integer.toString(correct));
        br.newLine();
        br.close();
    }

    public double subsetTrain(int size, String type) {
        ArrayList<Map<Attribute, String> > wholeData = trainDataSet.data;

        Collections.shuffle(trainDataSet.data);
        trainDataSet.data = new ArrayList<>(trainDataSet.data.subList(0, size));

        if(type.equals("n")) buildNaiveBayes();
        else buildMST();

        double res = testAccuracy();
        trainDataSet.data = wholeData;
        return res;
    }

    private double testAccuracy() {
        double correct = 0;
        double total = 0;

        for(int i=0; i<testDataSet.data.size(); i++) {
            Map<Attribute, String> record = testDataSet.data.get(i);

            // Find the max
            double res = -1;
            String resString = "";
            for(String yString : testDataSet.classLabel.possibleValues) {
                double tmpRes = trainDataSet.pyX(yString, record, tree);

                if(tmpRes > res) {
                    res = tmpRes;
                    resString = yString;
                }
            }

            total += 1;
            if(record.get(testDataSet.classLabel).equals(resString)) correct += 1;
        }

        return correct/total;
    }

}