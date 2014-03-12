package edu.cs236.lucene;

import edu.cs236.lucene.Controller;
import edu.cs236.lucene.LuceneResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.*;

/**
 * Created by jason on 3/6/14.
 */
public class SpanSearch {

    public String TAG = "spansearch";
    private SpanQuery query;
    private TopScoreDocCollector collector;
    protected Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
    private IndexReader reader;
    private IndexSearcher searcher;

    /**
     * Specific to this class
     **/

    private AtomicReader wrapper;
    //private HashMap<Integer, LuceneResult> results = new HashMap<Integer, LuceneResult>();
    private ArrayList<LuceneResult> results = new ArrayList<LuceneResult>();

    public SpanSearch() throws IOException{
        this.reader = Controller.LuceneController.getIndex();
        this.searcher = new IndexSearcher(Controller.LuceneController.getIndex());
        this.wrapper = SlowCompositeReaderWrapper.wrap(Controller.LuceneController.getIndex());
        this.collector = TopScoreDocCollector.create(Controller.LuceneController.getHitsPerPage(), true);
    }

    public void setQuery(String queryString, ArrayList<String> fields) {
        Log.d(TAG, queryString, 1);
        this.results.clear();
        String[] qs = new String[] {};
        qs = queryString.split(" ");
        SpanQuery[] terms = new SpanQuery[qs.length];
        int x = 0;
        for(String s : qs) {
            terms[x] = new SpanTermQuery(new Term("body", s));
            x++;
        }

        //this.query = new SpanTermQuery(new Term("body", queryString));
        this.query = new SpanNearQuery(terms, 5, true);
    }

    public void search() throws IOException{
        TopDocs results = this.searcher.search(this.query, Controller.LuceneController.hitsPerPage);
        //System.out.println("Length: " + results.scoreDocs.length);
        /**
         * Get the list of docIDs and LuceneResult
         */
        int x=0;
        for(ScoreDoc s : results.scoreDocs) {
            //this.results.put(s.doc, new LuceneResult(s.doc, s.score));
            //System.out.println("added a new result");
            this.results.add(new LuceneResult(s.doc, s.score));
            this.loadQueryResults(x, s.doc);
            //this.results.add(this.results.get(s.doc));
            x++;
        }
        this.sortResults();
    }

    private void loadQueryResults(int locInArray, int docId) throws IOException {
        String url = this.reader.document(docId).get("url");
        this.results.get(locInArray).setUrl(url);
        this.results.get(locInArray).setTerms(this.reader.getTermVector(docId, "body"));
        this.generateSnippets(locInArray);
        //this.results.get(doc).printTree();
    }

    /*
     *http://searchhub.org/2013/05/09/update-accessing-words-around-a-positional-match-in-lucene-4/
     */
    private void generateSnippets(int doc) throws IOException{
        int window = Controller.LuceneController.window;
        int docID = this.results.get(doc).getId();

        Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
        Spans spans = this.query.getSpans(wrapper.getContext(), new Bits.MatchAllBits(reader.numDocs()), termContexts);

        while(spans.next()) {
            Map<Integer, String> entries = new TreeMap<Integer, String>();
            //System.out.println("Doc: " + docID + " Start: " +
            //            spans.start() + " End: " +
            //            spans.end());

            int start = spans.start() - window;
            int end = spans.end() + window + 1;

            Terms content = this.reader.getTermVector(docID, "body");
            TermsEnum termsEnum = content.iterator(null);
            BytesRef term;

            while((term = termsEnum.next()) != null) {
                String s = new String(term.bytes, term.offset, term.length);
                DocsAndPositionsEnum positionsEnum = termsEnum.docsAndPositions(null, null);
                if(positionsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int i = 0;
                    int position;
                    StringBuilder context = new StringBuilder();
                    while(i < positionsEnum.freq() && (position = positionsEnum.nextPosition()) != -1) {
                        if(position >= start && position <= end){
                            entries.put(position, s);
                        }// if position
                        i++;
                    }// while position
                }// if docs
            }// while terms
            //System.out.println("Entries:" + entries);
            //System.out.println("Size of tree: " + entries.size());
            this.results.get(doc).addToArray(entries);
        }// while spans
        //this.results.get(doc).printArray();
        this.results.get(doc).generateSnippet();
        this.results.get(doc).formatResultsHTML();
    }



    public void generateSnippetsAlternate(int doc) throws IOException {
        //IndexReader reader = searcher.getIndexReader();
        //this is not the best way of doing this, but it works for the example.  See http://www.slideshare.net/lucenerevolution/is-your-index-reader-really-atomic-or-maybe-slow for higher performance approaches
        //AtomicReader wrapper = SlowCompositeReaderWrapper.wrap(this.searcher.getIndexReader());
        int docId = this.results.get(doc).getId();
        Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
        Spans spans = this.query.getSpans(this.wrapper.getContext(), new Bits.MatchAllBits(docId), termContexts);

        int window = Controller.LuceneController.window;

        while (spans.next()) {
            Map<Integer, String> entries = new TreeMap<Integer, String>();
            System.out.println("Doc: " + spans.doc() + " Start: " + spans.start() + " End: " + spans.end());

            int start = spans.start() - window;
            int end = spans.end() + window;

            Terms content = this.reader.getTermVector(spans.doc(), "body");
            TermsEnum termsEnum = content.iterator(null);
            BytesRef term;

            while ((term = termsEnum.next()) != null) {
                //could store the BytesRef here, but String is easier for this example
                String s = new String(term.bytes, term.offset, term.length);
                DocsAndPositionsEnum positionsEnum = termsEnum.docsAndPositions(null, null);

                if (positionsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                    int i = 0;
                    int position = -1;

                    while (i < positionsEnum.freq() && (position = positionsEnum.nextPosition()) != -1) {
                        if (position >= start && position <= end) {
                            entries.put(position, s);
                        }
                        i++;
                    }
                }
            }
            System.out.println("Entries:" + entries);
        }
    }

    public ArrayList<LuceneResult> getResults() {
        return this.results;
    }

    private void sortResults() {
        Collections.sort(this.results);
    }

    public void printResults() {
        System.out.println("");
        System.out.println("==========================");
        System.out.println("**** Sorted ArrayList ****");
        System.out.println("==========================");
        for(LuceneResult lr : this.results) {
            System.out.println("Doc: " + lr.getId() + "\tScore: " + lr.getScore());
            //System.out.println(lr.getHtml());
        }
    }
}
