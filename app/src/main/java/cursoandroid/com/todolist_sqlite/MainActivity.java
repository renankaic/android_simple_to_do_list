package cursoandroid.com.todolist_sqlite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import android.os.Bundle;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private EditText textoNovaTarefa;
    private ListView listaTarefas;
    private SQLiteDatabase bancoDeDados;

    private ArrayList<Integer> idsTarefas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        try {

            //Recupera componentes
            textoNovaTarefa = findViewById(R.id.textoId);
            Button botaoAdicionar = findViewById(R.id.botaoAdicionarId);

            //banco de dados
            bancoDeDados = openOrCreateDatabase("app-minhastarefas", MODE_PRIVATE, null);

            //criar as tabelas]
            bancoDeDados.execSQL("CREATE TABLE IF NOT EXISTS tarefas(id INTEGER PRIMARY KEY AUTOINCREMENT, tarefa VARCHAR , concluida VARCHAR)");

            //Botao salvar
            botaoAdicionar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String textoTarefa = textoNovaTarefa.getText().toString();

                    if( !textoTarefa.equals("")) {
                        salvarTarefa(textoTarefa);
                        textoNovaTarefa.setText("");
                    }
                }
            });

            //Criação da lista
            listaTarefas = findViewById(R.id.listViewId);
            listaTarefas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final int posicaoItem = position;

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.pergunta_tarefa)
                            .setPositiveButton(R.string.resposta_tarefa_concluir, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão para marcar a tarefa como concluída
                                    concluirTarefa(idsTarefas.get(posicaoItem));
                                }
                            })
                            .setNegativeButton(R.string.resposta_tarefa_excluir, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão para remover a tarefa
                                    removerTarefa( idsTarefas.get( posicaoItem ) );
                                }
                            })
                            .setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Botão cancelar para não fazer nada
                                }
                            });

                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            });

            //Lista as tarefas
            recuperarTarefas();

        } catch (Exception e){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Erro")
                    .setMessage(e.getMessage());

            AlertDialog alertDialog = builder.create();
            alertDialog.show();

            e.printStackTrace();
        }

    }

    private void salvarTarefa(String texto){

        try{
            if (!texto.equals("")) {

                bancoDeDados.execSQL("INSERT INTO tarefas (tarefa, concluida) VALUES ('" + texto + "', 'N')");
                Toast.makeText(this, "Tarefa salva com sucesso!", Toast.LENGTH_SHORT).show();
                recuperarTarefas();

            }  else {
                Toast.makeText(MainActivity.this, "Digite a descrição da tarefa antes de adicionar", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    private void recuperarTarefas(){

        try {

            //Recuperar as tarefas
            Cursor cursor = bancoDeDados.rawQuery("SELECT * FROM tarefas ORDER BY id DESC", null);

            //recuperar ids das colunas
            int indiceColunaId = cursor.getColumnIndex("id");
            int indiceColunaTarefa = cursor.getColumnIndex("tarefa");
            int indiceColunaConcluida = cursor.getColumnIndex("concluida");

            //cria o adaptador
            ArrayList<SpannableString> itens = new ArrayList<>();
            ArrayAdapter<SpannableString> itensAdaptador = new ArrayAdapter<>(
                    getApplicationContext(),
                    R.layout.items_list,
                    android.R.id.text1,
                    itens
            );

            idsTarefas = new ArrayList<>();
            listaTarefas.setAdapter(itensAdaptador);

            //Lista as tarefas - quando usa o rawquery ele fica parado no ultimo registro
            while (cursor.moveToNext()){

                String tarefaBanco = cursor.getString(indiceColunaTarefa);

                SpannableString textoTarefa = new SpannableString(tarefaBanco);

                //Aqui vejo se a tarefa foi concluída
                if ( cursor.getString(indiceColunaConcluida).equals("s") )
                {
                    textoTarefa.setSpan(new StrikethroughSpan(), 0, textoTarefa.length(), 0 );
                    textoTarefa.setSpan(new ForegroundColorSpan(Color.GRAY), 0, textoTarefa.length(), 0);
                }

                itens.add(textoTarefa);
                idsTarefas.add( Integer.parseInt( cursor.getString(indiceColunaId) ) );

            }

            cursor.close();

        } catch (Exception e){

            e.printStackTrace();
        }

    }

    private void removerTarefa(Integer id){
        try{

            bancoDeDados.execSQL("DELETE FROM tarefas WHERE id = " + id);
            Toast.makeText(MainActivity.this, "Tarefa removida com sucesso", Toast.LENGTH_SHORT).show();
            recuperarTarefas();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void concluirTarefa(Integer id){
        try{
            bancoDeDados.execSQL("UPDATE tarefas SET concluida = 's' WHERE id = " + id);
            recuperarTarefas();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
