package edu.cs236.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by jason on 3/6/14.
 */
public class IndexSearch {

    public String TAG = "indexsearch";
    private Query query;
    private TopScoreDocCollector collector;
    protected Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40, StandardAnalyzer.STOP_WORDS_SET);
    private IndexReader reader;
    private IndexSearcher searcher;
    private ScoreDoc[] scoreDoc;

    private ArrayList<LuceneResult> results = new ArrayList<LuceneResult>();

    public IndexSearch() throws IOException {
        this.reader = Controller.LuceneController.getIndex();
        this.searcher = new IndexSearcher(Controller.LuceneController.getIndex());
        this.collector = TopScoreDocCollector.create(Controller.LuceneController.hitsPerPage, true);
    }

    public void setQuery(String query, ArrayList<String> fields) throws IOException, ParseException {
        Log.d(TAG, query, 1);
        this.results.clear();
        this.query = new QueryParser(Version.LUCENE_40, "body", this.analyzer).parse(query);
        this.searcher.search(this.query, this.collector);
    }

    public void search() throws Exception{
        this.scoreDoc = this.collector.topDocs().scoreDocs;
        int x = 0;
        for(ScoreDoc s : this.scoreDoc) {
            this.results.add(new LuceneResult(s.doc, s.score));
            this.loadQueryResults(x, s.doc);
            x++;
        }
        this.sortResults();
        this.printSearchResults();
    }

    public void loadQueryResults(int locInArray, int docId) throws Exception {
        String url = this.reader.document(docId).get("url");
        this.results.get(locInArray).setUrl(url);
        this.results.get(locInArray).setTerms(this.reader.getTermVector(docId, "body"));
        this.results.get(locInArray).formatResultsHTML(false);
        //this.generateSnippets(locInArray);
    }

    public ArrayList<LuceneResult> getResults() {
        return this.results;
    }

    public void sortResults() {
        Collections.sort(this.results);
    }

    private void printSearchResults() {
        for(ScoreDoc s : this.scoreDoc) {
            System.out.println("ScoreDoc: " + s);
        }
    }

}
