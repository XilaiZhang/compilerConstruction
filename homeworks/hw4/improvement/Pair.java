import java.util.Comparator;
public class Pair{
    public Integer a;
    public Integer t;
    public String id="wtf";
    public int op;

    public Pair(Integer x, Integer y){
        a=x;
        t=y;
    }

    public Pair(Integer x, Integer y, String s, Integer i){
        a=x;
        t=y;
        id=s;
        op=i;
    }

    public Integer first(){
        return a;
    }

    public Integer second(){
        return t;
    }

    public String third(){
        return id;
    }

    public static Comparator<Pair> myComparator = new Comparator<Pair>(){
        public int compare(Pair p1, Pair p2){
            if(p1.op!=p2.op){
                if(p1.op==0 && p2.op==1){
                    if(p1.a != p2.t){
                        return p1.a - p2.t;
                    }
                    else{
                        return 1;
                    }
                }
                if(p1.op==1 && p2.op==0){
                    if(p1.t != p2.a){
                        return p1.t - p2.a;
                    }
                    else{
                        return -1;
                    }
                }
                return 142857;
            }
            else{
                if(p1.op==0){
                    return p1.a - p2.a;
                }
                else{
                    return p1.t - p2.t;
                }
            }
        }
    };
}