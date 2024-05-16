package com.itu.prom16.controller;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.itu.prom16.annotation.Controller;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

@WebServlet(name = "DefaultController", value = "/")
public class DefaultController extends HttpServlet {
    private String message;
    boolean checked = false ;
    List<String> nomController = new ArrayList<>();

    public void init() {
        message = "Hello World!";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");

        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>" + request.getRequestURI() + "</h1>");
        out.println("</body></html>");
        //request.getRequestURI();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    public void destroy() {
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        PrintWriter out = res.getWriter();

        if (!checked) {
            try {
                String controllerPackage = getServletConfig().getInitParameter("controllerChecker");
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                URL url = classLoader.getResource(controllerPackage);
                if (url != null) {
                    File directory = new File(url.getFile().replace("%20", " "));
                    if (directory.exists() && directory.isDirectory()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && file.getName().endsWith(".class")) {
                                    String className = file.getName().substring(0, file.getName().lastIndexOf('.'));
                                    Class<?> clazz = Class.forName(String.format("%s.%s", controllerPackage, className));
                                    if (clazz.isAnnotationPresent(Controller.class)) {
                                        nomController.add(clazz.getName());
                                    }
                                }
                            }
                        }
                    }
                }
                checked = true;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (checked){
            for (int i = 0; i < nomController.size(); i++) {
                out.println(nomController.get(i));
            }
        }
    }
}
