package com.example.serj.cleverbot;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class Adaptador extends ArrayAdapter<String> {

    private Context contexto;
    private ArrayList<String> lista;
    private int recurso;
    private static LayoutInflater i;

    public Adaptador(Context context, int resource, ArrayList<String> objects) {
        super(context, resource, objects);
        this.contexto = context;
        this.lista = objects;
        this.recurso = resource;
        this.i = (LayoutInflater) contexto.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh = null;
        if(convertView == null){
            convertView = i.inflate(recurso, null);
            vh = new ViewHolder();
            vh.tv1 = (TextView)convertView.findViewById(R.id.tv_texto);
            convertView.setTag(vh);
        }else{
            vh = (ViewHolder) convertView.getTag();
        }

        if(position % 2 == 0) {
            vh.tv1.setTextColor(Color.parseColor("#ff669900"));
        } else {
            vh.tv1.setTextColor(Color.parseColor("#ffcc0000"));
        }

        vh.tv1.setText(lista.get(position));

        return convertView;
    }

    static class ViewHolder{
        public TextView tv1;
    }
}