package edu.cs236.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * Created by jason on 3/11/14.
 */
public class MapReduceSearcher {

    public  Integer total_docs=0;
    //public  HashMap<String,Integer> length=new HashMap<String, Integer>();
    public HashMap<String,URLMap> ranking=new HashMap<String, URLMap>();
    public  Double lambda = 0.1;
    public Set<String> urls= new HashSet<String>();


    public MapReduceSearcher(){}

    class ResultsComparator implements Comparator<Results>
    {
        public int compare(Results a, Results b)
        {
            if (a.score > b.score)
                return -1;

            if (a.score == b.score)
                return 0;

            return 1;
        }

    }

    public String[] run(String Path,String Q) throws IOException {
        String input, Query;
        input=Path;
        Query=Q;

        //read index
        if(input.substring(input.length()-1)!="/") input=input+"/";

        File folder = new File(input);
        File[] listOfFiles = folder.listFiles();

        //uncomment and use this way if the word is the name of file and everything is combined
        process_length(input+"URL-length");
        String[] terms = Query.split(" ");
        if(terms.length>5) lambda=0.7;
        //System.out.println("Lambda="+lambda);
        for(String t: terms)
        {
            process_file(input+t);
        }

        int x = 0;
        int max = 10;
        List<String> array = new ArrayList<String>();
        for(Results r : query(Query)) {
            StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"result-row\" id=\"row-").append(x).append("\">\n");
            // form the url
            sb.append("<span class=\"result-url\" id=\"url-").append(x).append("\">\n");
            sb.append("<a href=\"").append(r.url).append("\" title=\"title\">");
            sb.append(r.url).append("</a>\n");
            sb.append("</span>\n");
            // Form the snippet
            sb.append("<span class=\"snippet\" id=\"snippet-").append(x).append("\">\n");
            sb.append("...Position: ").append(r.pos).append("...");
            sb.append("</span>\n");
            sb.append("<span class=\"result-rank\" id=\"rank-").append(x).append("\">\n");
            sb.append("Ranked at: ").append(r.score).append("</span>\n");

            sb.append("</div>");
            x++;

            array.add(sb.toString());
        }
        return array.toArray(new String[array.size()]);
    }

    public void process_file(String Path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(Path));
        try {
            String line = br.readLine();
            if(line==null) return;
            URLMap urlMap;
            Integer num_occurences;
            String word;
            String[]contents=line.split("\\s");
            //for(int i=0;i<contents.length;i++) System.out.println(contents[i].toString());
            word=contents[0];
            num_occurences=Integer.parseInt(contents[1]);
            urlMap=ranking.get(word);
            if(urlMap==null) urlMap=new URLMap();
            urlMap.total_occurrences+=num_occurences;
            num_occurences=urlMap.total_occurrences;
            //System.out.println(word + " " + num_occurences);
            line=br.readLine();
            while (line != null) {
                //System.out.println("Raw line: "+line);
                line=line.replaceAll("[^.\\w\\d]", " ");
                line=line.replaceFirst("\\s","");
                //System.out.println("Current line: "+line);
                String[]values=line.split("\\s");
                String url=values[0];
                if(urlMap==null) System.out.println("URL MAP NULL");
                //for(int i=0;i<values.length;i++) System.out.println(values[i].toString());
                URLInfo info=urlMap.urlmap.get(url);
                if(info==null) info=new URLInfo();
                //System.out.println(values[0]+ "\t"+values[1]+" "+values.length);
                //if(values[j]==" ") continue;
                //System.out.println("\t"+url+" "+values[j]);
                info.positions.add(Integer.parseInt(values[1]));
                info.doc_len=Integer.parseInt(values[2]);
                urls.add(url);
                //System.out.println("\t"+url+" "+score);
                urlMap.store(url,info.positions,new Float(0),info.doc_len);
                ranking.put(word,urlMap);
                line=br.readLine();
            }
            for(String s: urlMap.urlmap.keySet())
            {
                URLInfo info=urlMap.urlmap.get(s);
                if(info==null) info=new URLInfo();
                Float score=rank(s, num_occurences,info.positions.size(), info.doc_len);
                info.score=score;
                //System.out.println("URL: "+s + " Rank: "+info.score);
            }

            Float score=rank("default",num_occurences,0,1);
            //System.out.println("Default score: "+score);
            urlMap.store("default",new ArrayList<Integer>(),score,1);
            line = br.readLine();
        } finally {
            br.close();
        }
    }

    public void process_length(String Path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(Path));
        try{
            String line = br.readLine();
            while(line!=null)
            {
                String[] out=line.split("\\s");
                if (line.contains("URL-length"))
                {
                    total_docs=Integer.parseInt(out[out.length-1]);
                    //System.out.println("Total Documents: "+total_docs);
                }
                else
                {
                    //System.out.println("Unexpected lines(URL-Length should only have one line: "+line);
                    // if(out.length!=2) System.out.println("Error on: "+line);
                    //else
                    //{
                    //System.out.println("URL: "+out[0]+ " Length:"+out[1]);
                    //length.put(out[0], Integer.parseInt(out[1]));
                    //}
                }
                line=br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            br.close();
        }
    }

    public  float rank(String url, Integer total, int appears, Integer doc_len)
    {
        //Integer doc_len;
        //if(url!="default") doc_len= length.get(url);
        //else doc_len=1;
        if(doc_len<0)
        {
            //System.out.println("Error processing: "+url);
            return 0;
        }
        Double score=(1-lambda)*appears/doc_len+lambda*total/total_docs;
        return new Float(score);
    }

    public  ArrayList<Results> query(String Q){
        ArrayList<Results> res= new ArrayList<Results>();
        String[]contents=Q.split("\\s");
        HashMap<String,URLInfo> scores=new HashMap<String, URLInfo>();
        for(int i=0;i<contents.length;i++)
        {
            URLMap cur_word=ranking.get(contents[i]);
            if(cur_word==null)
            {
                //System.out.println("Could not find "+contents[i]);
                continue;
            }
            for(String url: urls)
            {
                if(url=="default") continue;
                //System.out.println("Processing term "+contents[i]+" on: "+url);
                URLInfo urlInfo=cur_word.urlmap.get(url);
                if(urlInfo==null)
                {
                    //System.out.println("Could not find info on "+url);
                    //continue;
                    urlInfo=cur_word.urlmap.get("default");
                }
                URLInfo queryurl=scores.get(url);
                if(queryurl==null)
                {
                    queryurl=new URLInfo();
                }
                queryurl.score+=urlInfo.score;
                if(urlInfo.positions.size()>0) queryurl.positions.add(urlInfo.positions);
                //System.out.println("New Score for "+url+":"+queryurl.score);
                scores.put(url,queryurl);
            }
        }
        for (String url: scores.keySet())
        {
            Results r= new Results();
            URLInfo info = scores.get(url);
            r.pos=info.positions;
            r.score=info.score;
            r.url=url;
            res.add(r);
        }
        Collections.sort(res, new ResultsComparator());
        for(Results r: res)
        {
            //System.out.println(r.url+" "+r.score);
            ArrayList a=r.pos;
            for(int i=0;i<a.size();i++) {
                //System.out.println("\t"+a.get(i));
            }
        }
        return res;
    }

    public class URLInfo {
        public ArrayList positions=new ArrayList<Integer>();
        public Float score=new Float(0);
        public Integer doc_len= new Integer(-1);
    }

    public class URLMap {
        public HashMap<String,URLInfo> urlmap= new HashMap<String,URLInfo>();
        public int total_occurrences=0;


        public void store(String url, ArrayList<Integer> l, Float rank, Integer doc_len)
        {
            URLInfo info=new URLInfo();
            info.positions=l;
            info.score=rank;
            info.doc_len=doc_len;
            urlmap.put(url,info);
        }
    }

    public class Results {
        public String url;
        ArrayList<Integer> pos=new ArrayList<Integer>();
        Float score;
    }

}
