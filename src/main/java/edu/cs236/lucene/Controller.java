package edu.cs236.lucene;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by jason on 3/4/14.
 */
public class Controller {

    public static enum LuceneController {
        Instance;
        private static DirectoryReader reader;
        public static final int hitsPerPage = 10;
        public static final int window = 10;
        public static final int snippetSize = 4;
        private static Directory directory;
        public static void openIndex(String path) throws IOException {
            reader = DirectoryReader.open(NIOFSDirectory.open(new File(path)));
        }
        public static void openDirectory(String path) throws IOException {
            directory = NIOFSDirectory.open(new File(path));
        }
        public static DirectoryReader getIndex() {return reader;}
        public static Directory getDirectory() {return directory;}
        public static void closeIndex() throws IOException {
            reader.close();
        }
        public static void closeDirectory() throws IOException {
            directory.close();
        }
        public static int getHitsPerPage() {return hitsPerPage;}

    }

/*****************************************************************************************/

    public boolean bIndex = true;
    public boolean bSpan = true;
    public boolean buildIndex = true;
    public boolean runSearch = true;
    public String TAG = "controller";
    private IndexSearch index;
    private SpanSearch span;
    private Indexer indexer;
    private String directory;
    private ArrayList<LuceneResult> indexResults = new ArrayList<LuceneResult>();
    private ArrayList<LuceneResult> spanResults = new ArrayList<LuceneResult>();
    private double searchTime;

    public Controller() throws IOException{
        /**
         * TODO: Change this for production
         */
        this.directory = "/media/database/lucene";
        //this.directory = "/Volumes/Virtual Machines/lucene";
        LuceneController.openIndex(this.directory);
    }

    public Controller (String dir) throws IOException {
        this.directory = dir;
        LuceneController.openIndex(this.directory);
    }

    public Controller(HashMap<String, String> args) throws Exception {
        this.directory = args.get("ll");
        if(buildIndex) {
            LuceneController.openDirectory(args.get("ll"));
            this.indexer = new Indexer(args.get("ll"), args.get("db"),
                args.get("server"), args.get("un"), args.get("pw"));
        }// end if
        if(runSearch) {
            LuceneController.openIndex(this.directory);
        }// end if
    }

    public void createSearcher() throws Exception {
        this.index = new IndexSearch();
        this.span = new SpanSearch();
    }

    public void indexOn(ArrayList<String> fields) throws Exception {
        //this.indexer = new Indexer(this.directory);

        this.indexer.setFlags(fields);
        // Max rows = 28017
        int max = 30000;
        int increment = 4000;
        double start = System.currentTimeMillis();
        for(int x=0;x<max;x+=increment) {
            Log.d(TAG, "Indexing " + x  + " of " + max, 2);
            //String query = "SELECT * FROM records ORDER BY id LIMIT " +
            //        Integer.toString(x) + ", " + Integer.toString(increment);
            //System.out.println(query);
            this.indexer.buildIndex("SELECT * FROM records ORDER BY id LIMIT "
                    + Integer.toString(x) + ", " + Integer.toString(increment));
        }
        double total = System.currentTimeMillis() - start;
        Log.d(TAG, "Index completed after taking: " + total, 1);
    }

    public void query(String query, ArrayList<String> args) throws Exception {
        Log.d(TAG, "Query string: " + query, 1);
        if(bIndex) {
            this.indexResults.clear();
            this.index.setQuery(query, args);
            this.index.search();
            this.indexResults = this.index.getResults();
            for(LuceneResult entry : this.index.getResults()) {
                System.out.println(entry.getHtml());
                System.out.println("*********************");
            }
        }

        if(bSpan) {
            this.spanResults.clear();
            double start = System.currentTimeMillis();
            this.span.setQuery(query, args);
            this.span.search();
            this.searchTime = System.currentTimeMillis() - start;

            Log.d(TAG, "Search Took: " + this.searchTime, 2);

            this.spanResults = this.span.getResults();

            //this.span.printResults();

            for(LuceneResult entry : this.span.getResults()) {
                //System.out.println("Doc: "+entry.getKey());
                //System.out.println("Snippet: " + entry.getValue().getSnippet());
                //System.out.println("#####################");
                System.out.println(entry.getHtml());
                //entry.getValue().printTree();
                System.out.println("---------------------");
            }
        }
    }

    public ArrayList<LuceneResult> getResults() {

        if(this.spanResults.size() > 0)
            return this.spanResults;
        else if(this.indexResults.size() > 0)
            return this.indexResults;
        else
            return new ArrayList<LuceneResult>();
    }
    public ArrayList<LuceneResult> getResults(int which) {
        if(which == 0) {
            return this.indexResults;
        } else if(which == 1) {
            return this.spanResults;
        } else {
            ArrayList<LuceneResult> r = new ArrayList<LuceneResult>();
            r.addAll(this.indexResults);
            r.addAll(this.spanResults);
            return r;
        }
    }

    public double getSearchTime() {return this.searchTime;}

    public void closeIndexer() throws Exception {
        this.indexer.closeWriter();
        this.indexer.closeDb();
        LuceneController.closeDirectory();
    }

    public void closeSearcher() throws Exception {
        LuceneController.closeIndex();
    }
}
