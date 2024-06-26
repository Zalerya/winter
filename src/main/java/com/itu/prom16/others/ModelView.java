package com.itu.prom16.others;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;

public class ModelView {
    String url;
    HashMap <String, Object> data;

    public ModelView() {

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public void addData (String cle, Object valeur) {
        if (this.getData() == null) {
            HashMap<String, Object> data = new HashMap<String, Object>();
            data.put(cle, valeur);
            this.setData(data);
        }
        else {
            this.getData().put(cle, valeur);
        }
    }

    public ModelView(String url) {
        this.url = url;
    }

    public ModelView(String url, HashMap<String, Object> data) {
        this.url = url;
        this.data = data;
    }

    public void execute (HttpServletRequest request, HttpServletResponse response) {
        if (this.getData() != null) {
            this.getData().forEach((cle, valeur) -> {
                request.setAttribute(cle, valeur);
            });
        }
    }

    public void executeAndRedirect (HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.execute(request, response);
        request.getRequestDispatcher(this.getUrl()).forward(request, response);
    }
}