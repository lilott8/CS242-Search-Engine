package edu.cs236.lucene;

import org.apache.lucene.index.Terms;
import org.apache.lucene.search.spans.Spans;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

/**
 * Created by jason on 3/7/14.
 */
public class LuceneResult implements Comparable<LuceneResult> {

    public String TAG = "result";
    private int docId;
    private float score;
    private ArrayList<Map<Integer, String>> snippetTreeArray = new ArrayList<Map<Integer, String>>();
    private ArrayList<String> snippetArray = new ArrayList<String>();
    private String snippet;
    private String url;
    private String html;
    private Terms terms;
    private Spans spans;

    public LuceneResult(int id, float s) {
        this.docId = id;
        this.score = s;
    }

    public void setDocId(int d) {
        this.docId = d;
    }

    public void setTerms(Terms t) {
        this.terms = t;
    }

    public void setScore(float s) {
        this.score = s;
    }

    public void addToArray(Map<Integer, String> s) {
        if (s.size() > 0)
        this.snippetTreeArray.add(s);
    }

    public void setUrl(String u) {
        this.url = u;
    }

    public void setSnippet(String s) {
        this.snippet = s;
    }

    public void setHtml(String s) {
        this.html = s;
    }

    public void setSpans(Spans s) {
        this.spans = s;
    }

    public void printArray() {
        for(Map<Integer, String> map : this.snippetTreeArray) {
            for(Map.Entry<Integer, String> entry : map.entrySet()) {
                String s = String.format("ArrayKey: %d\tMapKey: %d\tMapValue: %s", 1, entry.getKey(), entry.getValue());
                System.out.println(s);
            }
            System.out.println("====================");
        }
        System.out.println("SIze of array: " + this.snippetTreeArray.size());
    }

    public void generateSnippet() {
        for(Map<Integer, String> map : this.snippetTreeArray) {
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<Integer, String> entry : map.entrySet()) {
                sb.append(" ");sb.append(entry.getValue());
            }
            if(!this.snippetArray.contains(sb.toString())) {
                //System.out.println("Adding: " + sb.toString());
                this.snippetArray.add(sb.toString());
            }
        }
    }

    public int getId() {return this.docId;}
    public float getScore() {return this.score;}
    public String getSnippet() {return this.snippet;}
    public String getUrl() {return this.url;}
    public String getHtml() {return this.html;}
    public Spans getSpans() {return this.spans;}
    public Terms getTerms() {return this.terms;}


    public void formatResultsHTML() {
        Random r = new Random();
        int x = 0;
        int low = 0;
        int high = this.snippetArray.size();

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"result-row\" id=\"row-").append(this.docId).append("\">\n");
        // form the url
        sb.append("<span class=\"result-url\" id=\"url-").append(this.docId).append("\">\n");
        sb.append("<a href=\"").append(this.url).append("\" title=\"title\">");
        sb.append(this.url).append("</a>\n");
        sb.append("</span>\n");
        // Form the snippet
        sb.append("<span class=\"snippet\" id=\"snippet-").append(this.docId).append("\">\n");
        while(x < Controller.LuceneController.snippetSize) {
            sb.append("...").append(this.snippetArray.get(r.nextInt(high-low)+low));
            x++;
        }
        sb.append("</span>\n");
        sb.append("<span class=\"result-rank\" id=\"rank-").append(this.docId).append("\">\n");
        sb.append("Ranked at: ").append(this.score).append("</span>\n");

        sb.append("</div>");
        this.html = sb.toString();
    }

    /**
     * We want a descending sort, hence the odd value > 0 return -1
     * @param r luceneresult object
     * @return integer to determine the sorted order of a comparison
     * @throws ClassCastException if it's not a class, die
     */
    public int compareTo(LuceneResult r) throws ClassCastException {
        if(r == null)
            throw new ClassCastException("LuceneResult object expected");

        float value = this.score - r.getScore();
        if(value == 0) {
            return 0;
        } else if(value > 0) {
            return -1;
        } else {
            return 1;
        }

    }

}
