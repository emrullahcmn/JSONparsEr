package org.edeveloper;

class SharedPlace{
    private String line;
    private int lineCount;
    private boolean isLineParsed;
    private boolean isCompleted;

    SharedPlace(){
        line = "";
        isLineParsed = true;   // to enable first start for file reader thread
        isCompleted = false;   // whole file is readed?
        lineCount = 0;
    }

    synchronized void setLine(String line, int jumpedLines){
        while(!isLineParsed){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        if(line == null){
            isCompleted = true;
            this.line = null;
        }else{
            isLineParsed = false;
            this.line = line;
            lineCount += jumpedLines + 1;
        }

        notifyAll();
    }

    synchronized String getLine(){
        while(isLineParsed && !isCompleted){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        notifyAll();

        if(isCompleted){
            return null;
        }

        return this.line;
    }

    synchronized void lineReaded(){
        isLineParsed = true;
        notifyAll();
    }

    int getLineCount() {
        return lineCount;
    }
}
