package Ontology;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.json.JSONArray;
import org.json.JSONObject;

class Ontology {

    public static int findLongestPath(Map<String, List<String>> conceptList, List<String> parentList) {
        for (String parent : parentList) {
            List<String> list = conceptList.get(parent);
            if (list.size() != 0) { // if there are more parents/ancestors
                return 1 + findLongestPath(conceptList, list);
            }
        }
        return 1; // when we reach root node
    }

    public static void printLongestPath(Map<String, List<String>> conceptList, List<String> parentList) {
        for (String parent : parentList) {
            List<String> list = conceptList.get(parent);
            if (list.size() == 0) {
                System.out.print(parent);
            } else { // if there are more parents/ancestors
                System.out.print(parent + " -> ");
                printLongestPath(conceptList, list);
            } // when we reach root node
        }
    }

    public static void main(String[] args) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("./ontology.xml"));
            Map<String, List<String>> conceptList = new TreeMap<String, List<String>>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            Set<String> metricParentList = new TreeSet<String>();
            List<String> metricleafNodeList = new ArrayList<String>();

            // Fetch all concepts and their parents and add them to a Tree Map
            
            doc.getDocumentElement().normalize();
            // System.out.println(doc.getDocumentElement().getNodeName());
            NodeList list = doc.getElementsByTagName("Concept");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element e = (Element) node;
                    // System.out.println("Concept is " + e.getAttribute("name"));
                    String child = e.getAttribute("name");
                    List<String> parentList = new ArrayList<String>();
                    NodeList tempList = e.getElementsByTagName("DirectSuperConcepts");
                    for (int j = 0; j < tempList.getLength(); j++) {
                        Node tempNode = tempList.item(j);
                        if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element ep = (Element) tempNode;
                            NodeList tempList2 = ep.getElementsByTagName("ConceptReference");
                            for (int k = 0; k < tempList2.getLength(); k++) {
                                Node tempNode2 = tempList2.item(k);
                                if (tempNode2.getNodeType() == Node.ELEMENT_NODE) {
                                    Element ep2 = (Element) tempNode2;
                                    String parent = ep2.getAttribute("name");
                                    // System.out.println("Parent is " + ep2.getAttribute("name"));
                                    // System.out.println("========================");
                                    parentList.add(parent);
                                    metricParentList.add(parent);
                                }
                            }
                            conceptList.put(child, parentList);
                        }
                    }
                }
            }

            // Till here we have added each concept and their parent list to a TreeMap
            // Now we need to convert this data to JSON

            JSONArray jsonarr = new JSONArray();
            int total_concepts = 0;
            for (Map.Entry<String, List<String>> entry : conceptList.entrySet()) {
                String key = entry.getKey();
                List<String> value = entry.getValue();
                JSONObject json = new JSONObject();
                total_concepts++;
                json.put("name", key);
                json.put("parents", value);
                jsonarr.put(json);

            }

            // Writting the jsonObject into sample.json
            FileWriter fileWriter = new FileWriter("concepts.json");
            fileWriter.write(jsonarr.toString());
            fileWriter.close();

            // Till here, we have added the map contents on json format to concepts.json
            // now we need to compute the metrics asked in the document
            int count_root = 0;
            int parentCount = 0;
            for (Map.Entry<String, List<String>> entry : conceptList.entrySet()) {
                String concept = entry.getKey();
                if (!metricParentList.contains(concept)) {
                    metricleafNodeList.add(concept);
                }
                if (entry.getValue().size() == 0) {
                    count_root += 1;
                }
                parentCount += entry.getValue().size();
            }

            Map<String, Integer> pathMap = new TreeMap<String, Integer>();
            int longestPathLength = 0;
            for (String leaf : metricleafNodeList) {
                int pathLength = findLongestPath(conceptList, conceptList.get(leaf));
                pathMap.put(leaf, pathLength);
                // System.out.println("Path Length for leaf " + leaf + " is " + pathLength);
                if (pathLength > longestPathLength) {
                    longestPathLength = pathLength;
                }
            }

            // Now, since by observation, we can see that each node has a maximum of one
            // parent, we can say that the avg no of parents FOR A NODE is 1
            // (or rather close to 1, since object and personal item doesnt have a parent)

            // now, there are 2 options:

            // 1. we include object and personal item in out calculation of avg parent
            // which makes the avg to be 60/61 = 0.9836

            // 2. we dont include those two for the calculation
            // which makes the avg to be 60/59 = 1.0169

            // looking at the example values, since the avg no of parents was greater than
            // 1,
            // this slightly hints that the root values were not considered for the
            // calculation

            // however, i am printing both the values because i am not too sure about
            // the context of the question

            // therefore,
            double avg_parent1 = (double) parentCount / (total_concepts - count_root);
            double avg_parent2 = (double) parentCount / (total_concepts);

            //
            PrintStream o = new PrintStream(new File("stats.txt"));
            System.setOut(o);

            System.out.println("total number of concepts: " + total_concepts);
            System.out.println("\naverage number of parents: " + avg_parent1 + " OR " + avg_parent2);
            System.out.println("\nnumber of leaf concepts: " + metricleafNodeList.size());
            System.out.println("\nlongest path from a root to a leaf concept: " + longestPathLength + "\n");
            for (Map.Entry<String, Integer> entry : pathMap.entrySet()) {
                if (entry.getValue() == longestPathLength) {
                    String leaf = entry.getKey();
                    // System.out.println("\n\nFOR THE LEAF " + leaf + ":\nThe path is");
                    System.out.print(leaf + " -> ");
                    printLongestPath(conceptList, conceptList.get(leaf));
                    System.out.println();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
