import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jason on 3/3/14.
 */
public class Indexer {

    public String TAG = "indexer";
    private ArrayList<String> flags;
    private IndexWriter writer;
    private Connection connection;
    private Directory directory;
    private IndexWriterConfig conf;
    private StandardAnalyzer analyzer;
    private FieldType fieldType;


    public Indexer() throws IOException{
        this.flags = new ArrayList<String>();
        this.loadDefaults();
    }

    public Indexer(ArrayList<String> f, String path) throws IOException{
        this.flags = f;
        this.directory = FSDirectory.open(new File(path));
        this.loadDefaults();
    }

    public Indexer(String path, String db, String server, String user, String pw) throws Exception{
        this.directory = FSDirectory.open(new File(path));
        //this.openConnection(server, db, user, pw);
        this.loadDefaults();
    }

    public void loadDefaults() throws IOException{
        this.analyzer = new StandardAnalyzer(Version.LUCENE_40);
        this.conf = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        this.writer = new IndexWriter(this.directory, this.conf);
        //Store both position and offset information
        this.fieldType = new FieldType();
        this.fieldType.setStoreTermVectors(true);
        this.fieldType.setStoreTermVectorOffsets(true);
        this.fieldType.setStoreTermVectorPositions(true);
        this.fieldType.setStored(true);
        this.fieldType.setIndexed(true);
        this.fieldType.setTokenized(true);
    }

    private void openConnection(String server, String db, String u, String p) throws Exception {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        String connection = String.format("jdbc:mysql://%s:3306/%s", server, db);
        this.connection = DriverManager.getConnection(connection, u, p);
        this.connection.createStatement();
    }

    public void setFlags(ArrayList<String> f) {this.flags = f;}

    public void buildIndex(String query) throws Exception {
        Statement statement = this.connection.prepareStatement(query);
        ResultSet result = statement.executeQuery(query);

        while(result.next()) {
            Document d = new Document();
            // d.add(new IntField("id", result.getInt("ID"), Field.Store.NO));
            d.add(new TextField("id", result.getString("id"), Field.Store.NO));
            d.add(new Field("body", result.getString("body"), this.fieldType));
            d.add(new Field("url", result.getString("url"), this.fieldType));
            d.add(new Field("title", result.getString("title"), this.fieldType));
            // d.add(new Field("raw", result.getString("raw"), Field.Store.YES, Field.Index.ANALYZED));
            /* for(String s : this.flags) {
                d.add(new Field(s, result.getString(s), Field.Store.YES, Field.Index.ANALYZED));
            } */
            // this.writer.updateDocument(new Term("id", result.getString("ID")), d);
            // this.writer.updateDocument(new Term(), d);
            if (this.writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                // New index, so we just add the document (no old document can be there):
                writer.addDocument(d);
            } else {
                // Existing index (an old copy of this document may have been indexed) so
                // we use updateDocument instead to replace the old one matching the exact
                // path, if present:
                this.writer.updateDocument(new Term("id", result.getString("id")), d);
            }
        }
        // Close our results so we don't overflow our buffer
        result.close();
        System.gc();
    }

    public void closeWriter() throws IOException{
        this.writer.close();
    }
    public void closeDb() throws SQLException {
        this.connection.close();
    }
}
