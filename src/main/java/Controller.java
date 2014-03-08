import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jason on 3/4/14.
 */
public class Controller {

    public static enum LuceneController {
        Instance;
        private static DirectoryReader reader;
        public static final int hitsPerPage = 10;
        public static final int window = 2;
        public static final int snippetSize = 3;
        public static void openIndex(String path) throws IOException {
            reader = DirectoryReader.open(FSDirectory.open(new File(path)));
        }
        public static DirectoryReader getIndex() {return reader;}
        public static void closeIndex() throws IOException {
            reader.close();
        }
        public static int getHitsPerPage() {return hitsPerPage;}

    }

    public boolean bIndex = false;
    public boolean bSpan = true;
    public String TAG = "controller";
    private IndexSearch index;
    private SpanSearch span;
    private Indexer indexer;
    private String directory;

    public Controller(HashMap<String, String> args) throws Exception {
        this.directory = args.get("ll");
        this.indexer = new Indexer(args.get("ll"), args.get("db"),
                args.get("server"), args.get("un"), args.get("pw"));
        LuceneController.openIndex(this.directory);
        //this.searcher = new Searcher(args.get("ll"));
    }

    public void createSearcher() throws Exception {
        this.index = new IndexSearch();
        this.span = new SpanSearch();
    }

    public void indexOn(ArrayList<String> fields) throws Exception {
        this.indexer.setFlags(fields);
        // Max rows = 28017
        int max = 30000;
        int increment = 4000;
        for(int x=0;x<max;x+=increment) {
            Log.d(TAG, "Indexing " + x  + " of " + max, 2);
            this.indexer.buildIndex("SELECT * FROM records ORDER BY id LIMIT "
                    + Integer.toString(x) + ", " + Integer.toString(x+increment));
        }
        Log.d(TAG, "Index is done being built", 2);
    }

    public void query(String query, ArrayList<String> args) throws Exception {
        if(bIndex) {
            this.index.setQuery(query, args);
            this.index.search();
        }

        if(bSpan) {
            double start = System.currentTimeMillis();
            this.span.setQuery(query, args);
            this.span.search();
            double total = System.currentTimeMillis() - start;
            Log.d(TAG, "Search Took: " + total, 2);

            //this.span.printResults();

            for(LuceneResult entry : this.span.getResults()) {
                //System.out.println("Doc: "+entry.getKey());
                //System.out.println("Snippet: " + entry.getValue().getSnippet());
                //System.out.println("#####################");
                System.out.println(entry.getHtml());
                //entry.getValue().printTree();
                //System.out.println("---------------------");
            }
        }
    }


    public void closeIndexer() throws Exception {
        this.indexer.closeWriter();
        this.indexer.closeDb();
    }

    public void closeSearcher() throws Exception {
        LuceneController.closeIndex();
    }
}
