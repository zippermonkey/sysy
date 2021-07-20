package node;

import ir.ContextIR;
import ir.IR;
import ir.OpName;
import ir.VarInfo;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NIfStatement extends NStatement {
    public NExpression cond;
    public NStatement thenstmt;

    public NIfStatement() {
    }

    public NIfStatement(NExpression cond, NStatement thenstmt) {
        this.cond = cond;
        this.thenstmt = thenstmt;
    }

    public void print(int indentation, boolean end, PrintStream out) {
        this.printIndentation(indentation,end,out);
        out.println( "IfStatement");

        this.printIndentation(indentation + 1, false, out);
        out.println("Cond");
        cond.print(indentation + 2, false, out);

        this.printIndentation(indentation + 1, false, out);
        out.println("Then");
        thenstmt.print(indentation + 2, false, out);
    }

    public void generate_ir(ContextIR ctx, List<IR> ir) throws Exception {
        ctx.create_scope();
        String id=String.valueOf(ctx.get_id());
        CondResult cond=this.cond.eval_cond_runntime(ctx, ir);
        ir.add(new IR(cond.else_op,"IF_"+id+"_ELSE"));

        List<IR> ir_then = new ArrayList<>(),ir_else=new ArrayList<>();
        ContextIR ctx_then=ctx.clone(),ctx_else=ctx.clone();

        ctx_then.create_scope();
        this.thenstmt.generate_ir(ctx_then,ir_then);
        ctx_then.end_scope();

        ctx_else.id=ctx_then.id;
        ctx_else.create_scope();
        ctx_else.end_scope();

        ctx.id=ctx_else.id;

        List<IR> end=new ArrayList<>();
        end.add(new IR(IR.OpCode.LABEL,"IF_"+id+"_END"));

        for(int i=0;i<ctx_then.symbol_table.size();i++)
        {
            for(Map.Entry<String, VarInfo> s:ctx_then.symbol_table.get(i).entrySet())
            {
                if(s.getValue().name.equals(ctx_else.symbol_table.get(i).get(s.getKey()).name))
                {
                    VarInfo v=ctx.find_symbol(s.getKey());
                    assert(!v.is_array);
                    if(v.name.startsWith("%"))
                    {
                        v.name="%"+ctx.get_id();
                    }
                    ir_then.add(new IR(IR.OpCode.PHI_MOV,new OpName(v.name),new OpName(s.getValue().name),""));
                    ir_then.get(ir_then.size()-1).phi_block=end.iterator();
                    ir_else.add(new IR(IR.OpCode.PHI_MOV,new OpName(v.name),new OpName(ctx_else.symbol_table.get(i).get(s.getKey()).name),""));
                    ir_else.get(ir_else.size()-1).phi_block=end.iterator();
                }
            }
        }

        ir.addAll(ir_then);
        if(!ir_else.isEmpty())
        {
            ir.add(new IR(IR.OpCode.JMP,"IF_"+id+"_END"));
        }
        ir.add(new IR(IR.OpCode.LABEL,"IF_"+id+"_end"));
        ir.addAll(ir_else);
        ir.addAll(end);

        if(!ctx.loop_break_symbol_snapshot.empty())
        {
            Vector<Vector<Map<String,VarInfo> > > br=ctx.loop_break_symbol_snapshot.peek();
            Vector<Vector<Map<String,VarInfo> > > then_br=ctx_then.loop_break_symbol_snapshot.peek();
            Vector<Vector<Map<String,VarInfo> > > else_br=ctx_else.loop_break_symbol_snapshot.peek();
            br.addAll(then_br);
            br.addAll(else_br);
            Vector<Vector<Map<String,VarInfo> > > co=ctx.loop_continue_symbol_snapshot.peek();
            Vector<Vector<Map<String,VarInfo> > > then_co=ctx_then.loop_continue_symbol_snapshot.peek();
            Vector<Vector<Map<String,VarInfo> > > else_co=ctx_else.loop_continue_symbol_snapshot.peek();
            co.addAll(then_co);
            co.addAll(else_co);
        }
        ctx.end_scope();
    }
}
