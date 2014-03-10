package edu.cs236.lucene;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jason on 3/10/14.
 */
public class JSPMain {

    public ArrayList<LuceneResult> results = new ArrayList<LuceneResult>();
    public Controller controller;


    public JSPMain(String query) throws Exception {
        this.controller = new Controller();
        this.controller.createSearcher();

        this.controller.query(query, null);
        results = this.controller.getResults();
    }

    public String[] getResults() {
        List<String> r = new ArrayList<String>();
        for(LuceneResult lr : results) {
            r.add(lr.getHtml());
        }
        return r.toArray(new String[r.size()]);
    }

    public double getSearchTime() {return this.controller.getSearchTime();}

    public void closeSearcher() throws Exception {
        this.controller.closeSearcher();
    }
}
