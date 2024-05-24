package com.itu.prom16.controller;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itu.prom16.annotation.Controller;
import com.itu.prom16.annotation.Get;
import com.itu.prom16.others.ClassMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

//@WebServlet(name = "DefaultController", value = "/")
public class DefaultController extends HttpServlet {
    HashMap<String, ClassMethod> mesController = new HashMap();

    public void init() {
        try {
            String controllerPackage = getServletConfig().getInitParameter("controllerChecker");
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String dir = controllerPackage.replace(".", "/");
            URL url = classLoader.getResource(dir);
            if (url != null) {
                File directory = new File(url.getFile().replace("%20", " "));
                //System.out.println(directory.toString());
                if (directory.exists() && directory.isDirectory()) {
                    File[] files = directory.listFiles();
                    //System.out.println(files.length);
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && file.getName().endsWith(".class")) {
                                String className = file.getName().substring(0, file.getName().lastIndexOf('.'));
                                Class<?> clazz = Class.forName(String.format("%s.%s", controllerPackage, className));
                                if (clazz.isAnnotationPresent(Controller.class)) {
                                    Method[] lesMethodes = clazz.getDeclaredMethods();
                                    for (Method methode : lesMethodes) {
                                        if (methode.isAnnotationPresent(Get.class)) {
                                                Get get = methode.getAnnotation(Get.class);
                                                mesController.put(get.path(), new ClassMethod(clazz.getName(), methode.getName()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //response.setContentType("text/html");
        processRequest(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    public void destroy() {
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Hello
        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        String urlDemande = request.getRequestURI().replace(request.getContextPath(), "");
        out.println("<h1>" + urlDemande + "</h1>");
        if (mesController.containsKey(urlDemande)) {
            ClassMethod classMethod = mesController.get(urlDemande);
            out.println("<p>" + "TROUVE" + "</p>");
            out.println("<p>" + classMethod.getClazz() + "</p>");
            out.println("<p>" + classMethod.getMethod() + "</p>");
        }
        else {
            out.println("<p>" + "INEXISTANT" + "</p>");
        }

        out.println("</body></html>");
    }
}
