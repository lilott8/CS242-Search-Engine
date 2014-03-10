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

/**
 * Created by jason on 3/6/14.
 */
public class IndexSearch {

    private Query query;
    private TopScoreDocCollector collector;
    protected Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
    private IndexReader reader;
    private IndexSearcher searcher;
    private ScoreDoc[] scoreDoc;

    public IndexSearch() throws IOException {
        this.reader = Controller.LuceneController.getIndex();
        this.searcher = new IndexSearcher(Controller.LuceneController.getIndex());
        this.collector = TopScoreDocCollector.create(Controller.LuceneController.hitsPerPage, true);
    }

    public void setQuery(String query, ArrayList<String> fields) throws IOException, ParseException {
        this.query = new QueryParser(Version.LUCENE_40, "body", this.analyzer).parse(query);
        this.searcher.search(this.query, this.collector);
    }

    public void search() {
        this.scoreDoc = this.collector.topDocs().scoreDocs;
        this.printSearchResults();
    }

    private void printSearchResults() {
        for(ScoreDoc s : this.scoreDoc) {
            System.out.println("ScoreDoc: " + s);
        }
    }

}
