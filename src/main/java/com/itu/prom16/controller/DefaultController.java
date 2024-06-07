package com.itu.prom16.controller;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itu.prom16.annotation.Controller;
import com.itu.prom16.annotation.Get;
import com.itu.prom16.others.ClassMethod;
import com.itu.prom16.others.ModelView;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;

//@WebServlet(name = "DefaultController", value = "/")
public class DefaultController extends HttpServlet {
    HashMap<String, ClassMethod> mesController = new HashMap();

    public void peuplerList (String controllerPackage) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String dir = controllerPackage.replace(".", "/");
        URL url = classLoader.getResource(dir);
        if (url != null) {
            File directory = new File(url.getFile().replace("%20", " "));
            //System.out.println(directory.toString());
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".class")) {
                            String className = file.getName().substring(0, file.getName().lastIndexOf('.'));
                            Class<?> clazz = Class.forName(String.format("%s.%s", controllerPackage, className));
                            if (clazz.isAnnotationPresent(Controller.class)) {
                                Method[] lesMethodes = clazz.getDeclaredMethods();
                                for (Method methode : lesMethodes) {
                                    if (methode.isAnnotationPresent(Get.class)) {
                                        Get get = methode.getAnnotation(Get.class);
                                        if (mesController.containsKey(get.path())) {
                                            throw new Exception("Le chemin '" + get.path() + "' existe en double.");
                                        }
                                        else {
                                            mesController.put(get.path(), new ClassMethod(clazz.getName(), methode.getName()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    throw new Exception("Le dossier est vide.");
                }
            }
        }
    }

    public Object invokeMethode (ClassMethod classMethod) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class clazz = Class.forName(classMethod.getClazz());
        Method method = clazz.getMethod(classMethod.getMethod());
        Object result = method.invoke(clazz.newInstance());

        return result;
    }

    public void init() {
        try {
            String controllerPackage = getServletConfig().getInitParameter("controllerChecker");
            this.peuplerList(controllerPackage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        //response.setContentType("text/html");
        try {
            processRequest(request, response);
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }

    public void destroy() {
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        // Hello
        PrintWriter out = response.getWriter();

        String urlDemande = request.getRequestURI().replace(request.getContextPath(), "");

        if (urlDemande.equalsIgnoreCase("/")) {
            out.println("<html><body>");
            out.println("<h1>" + urlDemande + "</h1>");
            out.println("<p>" + "Page d'acceuil" + "</p>");
            out.println("</body></html>");
        }

        else if (mesController.containsKey(urlDemande)) {
            ClassMethod classMethod = mesController.get(urlDemande);

            if (this.invokeMethode(classMethod).getClass() == ModelView.class) {
                ModelView modelView = (ModelView) this.invokeMethode(classMethod);
                modelView.execute(request, response);
                request.getRequestDispatcher(modelView.getUrl()).forward(request, response);
            } else if (this.invokeMethode(classMethod).getClass() == String.class) {
                String result = (String) this.invokeMethode(classMethod);
                out.println("<html><body>");
                out.println("<h1>" + urlDemande + "</h1>");
                out.println("<p>" + "RESULT" + result + "</p>");
                out.println("</body></html>");
            }
            else {
                throw new Exception ("Le type de retour n'est pas pris en charge. (String et ModelView uniquement)");
            }
        }

        else {
            throw new Exception("L'URL est introuvable.");
        }
    }
}
