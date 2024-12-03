package org.edeveloper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

final public class JSONparsEr{
    private String input;
    private int len;
    private int indx;
    private boolean reportErrorIndexWithSymbol;
    private int maxLengthForReportingWithSymbol;
    private Object result;
    private Thread readerThread;
    private Thread parserThread;
    private SharedPlace sharedPlace;
    private final char[] escapes = {'"', '/', 'b', 'f', 'n', 'r', 't'};
    private final char[] escapesAsChar = {'\"', '/', '\b', '\f', '\n', '\r', '\t'};
    private String fullFilePath;

    public JSONparsEr(Path relativePath){
        this(Paths.get("").normalize().resolve(relativePath.normalize()).toAbsolutePath().toString());
    }

    public JSONparsEr(String fullFilePath){
        this.fullFilePath = fullFilePath;
        reportErrorIndexWithSymbol = false;
        maxLengthForReportingWithSymbol = 1000;
    }

    public void parse(){

        sharedPlace = new SharedPlace();
        result = null;

        try{
            parserThread = new Thread(() -> {
                try {
                    result = json();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    readerThread.interrupt();
                    e.printStackTrace();
                    //throw new RuntimeException(e);
                }
            });

            readerThread = new Thread(() -> {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(fullFilePath));
                    String line;
                    int jumpedLines = 0;
                    while((line = bufferedReader.readLine()) != null){
                        if(line.isEmpty()){
                            jumpedLines++;
                            continue;
                        }
                        sharedPlace.setLine(line, jumpedLines);
                        jumpedLines = 0;
                    }
                    sharedPlace.setLine(null, jumpedLines);

                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                    parserThread.interrupt();
                    e.printStackTrace();
                    //throw new RuntimeException(e);
                }
            });

            parserThread.start();
            readerThread.start();
            parserThread.join();
            readerThread.join();

        }catch (Exception e){
            //e.printStackTrace();
        }
    }

    public JsonList getJsonList(){
        JsonList jsonList = null;
        try {
            jsonList = new JsonList((ArrayList<Object>) result);
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonList;
    }
    public JsonMap getJsonMap(){
        JsonMap jsonMap = null;
        try {
            jsonMap = new JsonMap((HashMap<String, Object>) result);
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonMap;
    }

    public Object getJsonValue(){
        if(result instanceof HashMap<?,?> || result instanceof ArrayList<?>){
            throw new RuntimeException("getJsonValue can't be used for getting resulting parsed json if it doesn't consist of JUST string, int, etc, you should use getJsonMap or getJsonList as to your resulting parsed json.");
        }
        return result;
    }

    private Object json() throws Exception {
        Object res = value();
        if(indx != len ){
            reportErrorWithSymbol(indx);
            throw new Exception("expected EOF but couldn't find, line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));
        }
        return res;
    }

    private Object value() throws Exception {
        empty();
        char c = getNow("VALUE expected");
        if(c == '['){
            goBack();
            var arr = array();
            empty();
            return arr;
        }

        if(c == '{'){
            goBack();
            var object = object();
            empty();
            return object;
        }

        if(c=='"'){
            goBack();
            var string = string();
            empty();
            return string;
        }

        if(c == '-' || checkDigit(c)){
            goBack();
            var number = number();
            empty();
            return number;
        }

        if(c == 'f'){
            goBack();
            var negative = negative();
            empty();
            return negative;
        }

        if(c == 't'){
            goBack();
            var positive = positive();
            empty();
            return positive;
        }

        if(c == 'n'){
            goBack();
            var takeNull = takeNull();
            empty();
            return takeNull;
        }

        reportErrorWithSymbol(indx);
        throw new Exception("VALUE expected but couldn't be found, line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));
    }

    private char escape() throws Exception {
        char c = getNow("...");
        if(c == 'u'){
            goNext("hex error.");
            StringBuilder str = new StringBuilder();
            for(int i = 0; i<4; i++){
                str.append(hex());
            }
            return (char)Integer.parseInt(str.toString(), 16);
        }

        for(int i = 0; i<escapes.length; i++){
            if(c == escapes[i]){
                indx++;
                return escapesAsChar[i];
            }
        }

        throw new Exception("in escaping, line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));

    }


    private char hex() throws Exception {
        char c = getNow("...");
        if(c >= 'A' && c <= 'F' || c>='a' && c<= 'f' || c >= '0' && c <= '9'){
            indx++;
            return c;
        }
        reportErrorWithSymbol(indx);
        throw new Exception("wrong hex value, line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));
    }

    private boolean checkDigit(char c){
        for(char ch = '0'; ch <= '9'; ch++)
            if(c==ch) {
                return true;
            }
        return false;
    }

    private Object number() throws Exception {
        goNext("digit expected");
        if(!checkDigit(getNow("...")) && getNow("...") != '-'){
            throw new Exception("number expected, line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));
        }

        String intNow ="";
        String posNeg = "";
        if(getNow("...") == '-'){
            goNext("expected integer");
            posNeg = "-";
        }

        intNow = integer();
        if(intNow.charAt(0) == '0' && intNow.length() > 1){
            throw new Exception("error, not an integer, line at:"+sharedPlace.getLineCount() + " char at:" + (indx));
        }

        if(indx == len){
            long lng = Long.parseLong(posNeg+intNow);
            if( lng > Integer.MAX_VALUE){
                return lng;
            }
            return Integer.parseInt(posNeg+intNow);
        }

        String intFraction="";

        if(input.charAt(indx) == '.'){
            goNext("number expected after .");
            intFraction = "."+integer();
        }

        if(indx == len){
            return Double.parseDouble(posNeg+intNow+intFraction);
        }

        String exponentPart = "";

        char c = input.charAt(indx);
        if(c == 'e' || c == 'E'){
            goNext("integer or sign expected after e or E");

            if(input.charAt(indx) == '-' || input.charAt(indx) == '+'){
                char sign = input.charAt(indx);
                goNext("integer expected after sign");
                exponentPart += c+""+sign + integer();
            }else if(checkDigit(input.charAt(indx))){
                exponentPart += c+""+integer();
            }else{
                throw new Exception("unexpected character after" + c + "line at:" +sharedPlace.getLineCount() + ", char at:"+(indx+1));
            }
        }


        if(!intFraction.isEmpty() || !exponentPart.isEmpty())
            return Double.parseDouble(posNeg+intNow + intFraction + exponentPart);

        long lng = Long.parseLong(posNeg+intNow + intFraction + exponentPart);
        if( lng > Integer.MAX_VALUE){
            return lng;
        }
        return Integer.parseInt((posNeg+intNow + intFraction + exponentPart));
    }

    private String integer(){  // long donuyordu, String donduruyoruz artık
        StringBuilder str = new StringBuilder();
        while(indx < len && checkDigit(input.charAt(indx))){
            str.append(input.charAt(indx));
            indx++;
        }
        //return Long.parseLong(str.toString());
        return str.toString();
    }

    private ArrayList<Object> array() throws Exception {
        goNext("[ expected");
        if(getNow("...") != '['){
            reportErrorWithSymbol(indx);
            throw new Exception("Error happened in array, [ expected, line at:"+sharedPlace.getLineCount() + ", char at:"+(indx+1) );
        }

        increaseIndex();

        empty();

        if(getNow("...") == ']'){
            indx++;
            return new ArrayList<Object>();
        }

        ArrayList<Object> seq = arraySequence();

        if(getNow("] expected") != ']'){
            reportErrorWithSymbol(indx);
            throw new Exception("] or , expected, line at:"+sharedPlace.getLineCount() + ", char at:"+(indx+1) );
        }

        increaseIndex();
        return seq;
    }

    private ArrayList<Object> arraySequence() throws Exception {
        ArrayList<Object> seq = new ArrayList<>();
        Object o = value();
        seq.add(o);

        while(indx != len && input.charAt(indx) == ','){
            indx++;
            o = value();
            seq.add(o);
        }
        return seq;
    }

    public HashMap<String, Object> object() throws Exception {
        goNext("{ expected");

        if(getNow("...") != '{'){
            reportErrorWithSymbol(indx);
            throw new Exception("Error happened in object, { expected, line at:"+sharedPlace.getLineCount() + ", char at:"+(indx+1) );
        }

        increaseIndex();
        empty();

        HashMap<String, Object> hashMap = new HashMap<>();

        if(getNow("} or , expected") == '}'){
            increaseIndex();
            return hashMap;
        }

        var pair = member();
        hashMap.put(pair.first, pair.second);
        while(indx != len && input.charAt(indx) == ','){
            indx++;
            empty();
            pair = member();
            hashMap.put(pair.first, pair.second);
        }

        if(getNow("} expected") != '}'){
            throw new Exception("Error in object definition, line at:"+sharedPlace.getLineCount() + ", char at:"+ (indx+1));
        }

        increaseIndex();

        return hashMap;
    }


    private Pair member() throws Exception{
        Pair pair = new Pair();
        goBack();

        pair.first = string();
        empty();

        if(getNow(": expected") != ':'){
            throw new Exception(": expected but could't be found, line at:"+sharedPlace.getLineCount() + ", char at:"+(indx+1));
        }

        increaseIndex();
        pair.second = value();
        return pair;
    }



    private String string() throws Exception {
        goNext("\" expected");

        if(getNow("...") != '"'){
            throw new Exception("\" expected but couldnt be found, line at: "+ sharedPlace.getLineCount()+ ", char at:" + (indx + 1));
        }

        goNext("char expected");

        String str = charSequence();

        goNext("\" expected");

        if(getNow("...") != '"'){
            reportErrorWithSymbol(indx);
            throw new Exception("String wasnt terminated correctly.. expected \" but not found, line at:"+ sharedPlace.getLineCount() + " char at:"+ (indx+1));
        }

        increaseIndex();

        return str;
    }

    private String charSequence() throws Exception {
        StringBuilder s = new StringBuilder();
        while(indx < len && input.charAt(indx) != '"'){
            if(input.charAt(indx) == '\\'){
                goNext("in escaping");
                s.append(escape());
                continue;
            }
            s.append(input.charAt(indx));
            indx++;
        }

        goBack();

        return s.toString();
    }

    private boolean negative() throws Exception {

        goNext("letter f expected");
        var c = getNow("...");
        if(c == 'f' && indx+4 < len){
            StringBuilder str = new StringBuilder();
            for(int i = indx; i <= indx+4; i++){
                str.append(input.charAt(i));
            }
            if(str.toString().equals("false")){
                indx += 4;
                increaseIndex();
                return false;
            }
        }
        reportErrorWithSymbol(indx);
        throw new Exception("expected false line at: "+ sharedPlace.getLineCount()+ ", char at:" + (indx+1));
    }

    private boolean positive() throws Exception{

        goNext("letter t expected");

        var c = getNow("...");
        if(c == 't' && indx+3 < len){
            StringBuilder str = new StringBuilder();
            for(int i = indx; i <= indx+3; i++){
                str.append(input.charAt(i));
            }
            if(str.toString().equals("true")){
                indx += 3;
                increaseIndex();
                return true;
            }
        }
        reportErrorWithSymbol(indx);
        throw new Exception("expected true, line at: "+ sharedPlace.getLineCount() +", char at:"+(indx+1));
    }

    private Object takeNull() throws Exception{
        goNext("letter n expected");

        var c = getNow("...");
        if(c == 'n' && indx+3 < len){
            StringBuilder str = new StringBuilder();
            for(int i = indx; i <= indx+3; i++){
                str.append(input.charAt(i));
            }
            if(str.toString().equals("null")){
                indx += 3;
                increaseIndex();
                return null;
            }
        }
        reportErrorWithSymbol(indx);
        throw new Exception("expected null, line at:" + sharedPlace.getLineCount()+ ", char at:" + (indx+1));
    }

    private void empty(){

        if(indx == len){
            sharedPlace.lineRead();
            String nLine = sharedPlace.getLine();
            indx = 0;
            if(nLine == null){
                len = 0;
                input = null;
                return;
            }
            len = nLine.length();
            input = nLine;
        }

        if(checkEmpty(input.charAt(indx))){
            indx++;
            empty();
        }
    }

    private char getNow(String errMsg) throws Exception{
        if(indx == len){
            reportErrorWithSymbol(indx);
            throw new Exception("Error, "+errMsg + ", line at:" + sharedPlace.getLineCount() +  ", char at: " + indx);
        }
        return input.charAt(indx);
    }

    private void goNext(String errMsg) throws Exception {
        if(indx+1 == len){
            reportErrorWithSymbol(indx+1);
            throw new Exception("Error, "+ errMsg + ", line at:" + sharedPlace.getLineCount() + ", char at: " + (indx+1));
        }
        indx++;
    }

    private void goBack(){
        indx--;
    }

    private void increaseIndex(){
        if(indx + 1 == len){
            sharedPlace.lineRead();
            String nLine = sharedPlace.getLine();
            indx = 0;
            if(nLine == null){
                len = 0;
                input = null;
                return;
            }
            len = nLine.length();
            input = nLine;
            return;
        }
        indx++;
    }

    private boolean checkEmpty(char c){
        if(c == ' ' || c == '\r' || c == '\n') return true;
        return false;
    }

    private void reportErrorWithSymbol(int ix){
        if(reportErrorIndexWithSymbol){
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i<10000; i++) sb.append(" ");
            sb.setCharAt(ix, '^');
            System.out.println(sb);
        }
    }

    public boolean getReportErrorIndex() {
        return reportErrorIndexWithSymbol;
    }

    public void setReportErrorIndex(Boolean reportErrorIndexWithSymbol) {
        this.reportErrorIndexWithSymbol = reportErrorIndexWithSymbol;
    }

    public int getMaxLengthForReportingWithSymbol(){
        return maxLengthForReportingWithSymbol;
    }
    public void setMaxLengthForReportingWithSymbol(int maxLengthForReportingWithSymbol){
        this.maxLengthForReportingWithSymbol = maxLengthForReportingWithSymbol;
    }
    public String getFullFilePath() {
        return fullFilePath;
    }
    public void setFullFilePath(String fullFilePath) {
        this.fullFilePath = fullFilePath;
    }

    public Object getRawResult() {
        return result;
    }
}
