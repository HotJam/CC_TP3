public final class Chronometer{
    private long begin, end;

    public void start(){
        begin = System.currentTimeMillis();
    }

    public void stop(){
        end = System.currentTimeMillis();
    }

    public long getFinalTime() {
        return end-begin;
    }

    public long getCurrentTime(){
      return System.currentTimeMillis();
    }

    public long getMilliseconds() {
        return end-begin;
    }

    public double getSeconds() {
        return (end - begin) / 1000.0;
    }

    public double getMinutes() {
        return (end - begin) / 60000.0;
    }

    public double getHours() {
        return (end - begin) / 3600000.0;
    }
}
