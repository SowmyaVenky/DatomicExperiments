package com.gssystems.datomic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class FormatEDNFilesToRemoveId {
    public static void main(String[] args) throws Exception {
        FileReader fr = new FileReader(
                "C:\\Venky\\DatomicExperiments\\DatomicExperiments\\src\\main\\resources\\seattle\\seattle-data.edn");
        BufferedReader br = new BufferedReader(fr);
        String aLine = null;

        // The :neighborhood/name is coming on a previous line, and we need to append it
        // to the end of the community line.

        List<String> nbrHoodLine = new ArrayList<String>();

        while ((aLine = br.readLine()) != null) {
            if (aLine.trim().length() > 0) {
                String[] tokens = aLine.split(",");
                StringBuffer b = new StringBuffer();

                for (String a : tokens) {
                    if (a.indexOf(":db/id") == -1 && a.indexOf("#db/id[:db.part/user") == -1) {
                        b.append(a + ",");
                    }
                }

                String t1 = b.substring(0, b.toString().length() - 1);

                if( t1.indexOf(":neighborhood/name") > -1 ) {
                    //put in list.
                    nbrHoodLine.add(t1);
                    continue;
                }

                if( t1.indexOf(":community/category") > -1 ) {
                    //append from array to end of line and remove the cached line.
                    t1 = t1 + nbrHoodLine.get(0);
                    nbrHoodLine.clear();
                }

                if (t1.endsWith("}")) {
                    System.out.println(t1);
                } else {
                    System.out.println(t1 + "}");
                }

            }
        }
        br.close();
    }
}
