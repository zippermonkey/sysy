import java.io.*;
import java.util.*;

import node.*;
import parser.*;
import ir.*;

public class Main {

    static public void main(String argv[]) {
        try {
            parser p = new parser(new Lexer(new FileReader(argv[0])));
            Object result = p.parse().value;
            NRoot root = p.root;
            root.print(0,false,System.out);
            List<IR> ir = new ArrayList<>();
            ContextIR ctx = new ContextIR();
            root.generate_ir(ctx, ir);
            for(IR i:ir){
                i.print(System.out, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}