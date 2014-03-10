package edu.cs236.lucene;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by jason on 3/4/14.
 */
public class Main {

    public static ArrayList<LuceneResult> results = new ArrayList<LuceneResult>();

    public static final int pageSize = 10;

    public static void searchFor(String query) throws Exception {
        Controller controller = new Controller();
        controller.createSearcher();
        controller.query(query, new ArrayList<String>());
        results = controller.getResults();
        controller.closeIndexer();
    }

    public static ArrayList<String> printPagatedResults(int pageNumber) {
        ArrayList<String> r= new ArrayList<String>();
        for(int x = pageSize * pageNumber; x<(pageSize * pageNumber) + pageSize;x++){
            r.add(results.get(x).getHtml());
        }
        return r;
    }

    public static void main(String[] args) throws Exception {
        HashMap<String, String> arguments = parseArgs(args);
        Controller controller = new Controller(arguments, true, true);

        String input;
        String query = "";
        ArrayList<String> eval;
        Scanner scanner = new Scanner(System.in);
        boolean acceptInput = true;

        do{
            System.out.print("Enter your command: ");
            input = scanner.nextLine();
            eval = new ArrayList<String>(Arrays.asList(input.split(" ")));
            ArrayList<Integer> keys = new ArrayList<Integer>();
            String command = eval.remove(0);

            if(command.equalsIgnoreCase("search")) {
                // create the searcher
                controller.createSearcher();
                // example: search jason rocks -body
                if(eval.size() > 1) {
                    controller.query(eval.remove(0), eval);
                } else {
                    eval.addAll(new ArrayList<String>(Arrays.asList("body", "title", "url")));
                    controller.query(eval.remove(0), eval);
                }
                //controller.closeSearcher();
                //controller.printSearchResults();
            } else if(command.equalsIgnoreCase("index")) {
                // example: index -body -url -title
                if(eval.size() > 0) {
                    controller.indexOn(eval);
                } else {
                    eval.addAll(new ArrayList<String>(Arrays.asList("body", "title", "url")));
                    controller.indexOn(eval);
                }
                //controller.closeIndexer();
            } else if(command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit")) {
                acceptInput = false;
            } else {
                System.out.println("Invalid command");
            }
            System.out.flush();
        } while(acceptInput);

        controller.closeSearcher();
        controller.closeIndexer();

        System.out.println("Thanks for searching!");
    }

    public static HashMap<String, String> parseArgs(String[] args) {
        HashMap<String, String> arguments = new HashMap<String, String>();
        HashMap<String, String> defaults = new HashMap<String, String>();
        //defaults.put("ll", "/media/database/lucene");
        defaults.put("ll", "/Volumes/Virtual Machines/lucene");
        defaults.put("un", "root");
        defaults.put("pw", "root");
        defaults.put("server", "c");
        defaults.put("db", "Spidey");
        // check for file, user, password, server, database
        for(String s : args) {
            if(s.equalsIgnoreCase("ll")) {
                if(new File(s).exists()) {
                    arguments.put("ll", s);
                }
            }
            if(s.equalsIgnoreCase("un")){arguments.put("un", s);}
            if(s.equalsIgnoreCase("pw")){arguments.put("pw", s);}
            if(s.equalsIgnoreCase("server")){arguments.put("server", s);}
            if(s.equalsIgnoreCase("db")){arguments.put("db", s);}
        }
        /*
        * Load the defaults for any missing keys in our arguments
         */
        for(Map.Entry<String, String> def : defaults.entrySet()) {
            if(!arguments.containsKey(def.getKey())) {
                arguments.put(def.getKey(), def.getValue());
            }
        }

        for(Map.Entry<String, String> arg : arguments.entrySet()) {
            System.out.println(arg.getKey() + ": " + arg.getValue());
        }
        return arguments;
    }
}
