package com.itu.prom16.controller;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

import com.itu.prom16.annotation.Controller;
import com.itu.prom16.annotation.Get;
import com.itu.prom16.annotation.Input;
import com.itu.prom16.annotation.Param;
import com.itu.prom16.others.ClassMethod;
import com.itu.prom16.others.ModelView;
import com.itu.prom16.others.Tools;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

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

    public Object invokeMethodeObject (ClassMethod classMethod, HttpServletRequest request, HttpServletResponse response) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, ServletException, IOException {
        Class clazz = Class.forName(classMethod.getClazz());
        Method method = clazz.getMethod(classMethod.getMethod());
        if (this.invokeMethode(classMethod).getClass() == ModelView.class) {
            ModelView modelView = (ModelView) this.invokeMethode(classMethod);
            modelView.executeAndRedirect(request, response);
            return null;
        }
        else {
            int nbParam = method.getParameterCount();
            Object[] params = new Object[nbParam];
            Parameter[] paramsFromMethod = method.getParameters();

            for (int i = 0; i < nbParam; i++) {
                Parameter individualParam = paramsFromMethod[i];
                if (individualParam.isAnnotationPresent(Param.class)) {
                    Param detailsParam = individualParam.getAnnotation(Param.class);
                    params[i] = individualParam.getType().cast(request.getParameter(detailsParam.name()));
                }
                else {
                    params[i] = individualParam.getType().cast(request.getParameter(individualParam.getName()));
                }
            }

            return method.invoke(clazz, params);
        }
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
        sprint7(request, response);
    }

    public void sprint7(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        String urlDemande = request.getRequestURI().replace(request.getContextPath(), "");

        if (urlDemande.equalsIgnoreCase("/")) {
            out.println("<html><body>");
            out.println("<h1>" + urlDemande + "</h1>");
            out.println("<p>" + "Page d'acceuil" + "</p>");
            out.println("</body></html>");
        } else {
            if (mesController.containsKey(urlDemande)) {
                ClassMethod classMethod = mesController.get(urlDemande);
                Class<?> clazz = Class.forName(classMethod.getClazz());
                Method[] methods = clazz.getDeclaredMethods();
                Method methodToInvoke = null;

                for (Method method : methods) {
                    if (method.getName().compareTo(classMethod.getMethod()) == 0 && method.isAnnotationPresent(Get.class)) {
                        if (method.getAnnotation(Get.class).path().equalsIgnoreCase(urlDemande)) {
                            methodToInvoke = method;
                            break;
                        }
                    }
                }

                int nbParameter = methodToInvoke.getParameterCount();
                Object[] params = new Object[nbParameter];
                Parameter[] parametersDetails = methodToInvoke.getParameters();

                for (int i = 0; i < nbParameter; i++) {
                    Parameter parameter = parametersDetails[i];
                    Class<?> parameterType = parameter.getType();
                    if (Tools.isCustomClass(parameterType)) {
                        Object paramObject = parameterType.newInstance();
                        Field[] fields = parameterType.getDeclaredFields();

                        for (Field field : fields) {
                            String paramName;
                            if (field.isAnnotationPresent(Input.class)) {
                                paramName = field.getAnnotation(Input.class).name();
                            } else {
                                paramName = field.getName();
                            }
                            String paramValue = request.getParameter(paramName);
                            if (paramValue != null) {
                                Method setter = parameterType.getMethod("set" + capitalize(field.getName()), field.getType());
                                setter.invoke(paramObject, field.getType().cast(paramValue));
                            }
                        }
                        params[i] = paramObject;
                    } else {
                        String paramValue = request.getParameter(parameter.getName());
                        params[i] = parameterType.cast(paramValue);
                    }
                }

                Object result = methodToInvoke.invoke(clazz.newInstance(), params);
                if (result instanceof ModelView) {
                    ModelView modelView = (ModelView) result;
                    modelView.execute(request, response);
                    request.getRequestDispatcher(modelView.getUrl()).forward(request, response);
                } else if (result instanceof String) {
                    String resultString = (String) result;
                    out.println("<html><body>");
                    out.println("<h1>" + urlDemande + "</h1>");
                    out.println("<p>" + "RESULT" + resultString + "</p>");
                    out.println("</body></html>");
                } else {
                    throw new Exception("Le type de retour n'est pas pris en charge. (String et ModelView uniquement)");
                }
            } else {
                throw new Exception("L'URL est introuvable.");
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    public void sprint6 (HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();

        String urlDemande = request.getRequestURI().replace(request.getContextPath(), "");

        if (urlDemande.equalsIgnoreCase("/")) {
            out.println("<html><body>");
            out.println("<h1>" + urlDemande + "</h1>");
            out.println("<p>" + "Page d'acceuil" + "</p>");
            out.println("</body></html>");
        }

        else {
            if (mesController.containsKey(urlDemande)) {
                ClassMethod classMethod = mesController.get(urlDemande);
                Class clazz = Class.forName(classMethod.getClazz());
                Method[] methods = clazz.getDeclaredMethods();
                Method methodToInvoke = null;

                for (Method method : methods) {
                    if (method.getName().compareTo(classMethod.getMethod()) == 0
                            && method.isAnnotationPresent(Get.class)) {
                        if (method.getAnnotation(Get.class).path().equalsIgnoreCase(urlDemande)) {
                            methodToInvoke = method;
                            break;
                        }
                    }
                }

                int nbParameter = methodToInvoke.getParameterCount();
                if (nbParameter == 0) {
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
                    Parameter[] parametersDetails = methodToInvoke.getParameters();
                    Object[] params = new Object[nbParameter];
                    for (int i = 0; i < nbParameter; i++) {
                        Parameter parameter = parametersDetails[i];
                        String parameterDataString = null;
                        if (parameter.isAnnotationPresent(Param.class)) {
                            parameterDataString = request.getParameter(parameter.getAnnotation(Param.class).name());
                        }
                        else {
                            parameterDataString = request.getParameter(parameter.getName());
                        }
                        params[i] = parameter.getType().cast(parameterDataString);
                    }
                    if (methodToInvoke.invoke(clazz.newInstance(), params) == ModelView.class) {
                        ModelView modelView = (ModelView) this.invokeMethode(classMethod);
                        modelView.execute(request, response);
                        request.getRequestDispatcher(modelView.getUrl()).forward(request, response);
                    } else if (methodToInvoke.invoke(clazz.newInstance(), params) == String.class) {
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
            }

            else {
                throw new Exception("L'URL est introuvable.");
            }
        }
    }

    public void sprint5 (HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();

        String urlDemande = request.getRequestURI().replace(request.getContextPath(), "");

        if (urlDemande.equalsIgnoreCase("/")) {
            out.println("<html><body>");
            out.println("<h1>" + urlDemande + "</h1>");
            out.println("<p>" + "Page d'acceuil" + "</p>");
            out.println("</body></html>");
        }

        else {
            if (mesController.containsKey(urlDemande)) {
                ClassMethod classMethod = mesController.get(urlDemande);
                Class clazz = Class.forName(classMethod.getClazz());

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
}
