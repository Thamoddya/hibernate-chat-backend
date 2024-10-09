/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.HibernateUtil;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

/**
 *
 * @author thamoddya
 */
@WebServlet(name = "SignIn", urlPatterns = {"/SignIn"})
public class SignIn extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", false);

        try {

            JsonObject jsonObject = gson.fromJson(req.getReader(), JsonObject.class);
            String mobile = jsonObject.get("mobile").getAsString();
            String password = jsonObject.get("password").getAsString();

            if (mobile.isEmpty()) {
                responseJson.addProperty("message", "Please enter mobile number");
            } else if (password.isEmpty()) {
                responseJson.addProperty("message", "Please enter your password");
            } else {
                Session session = HibernateUtil.getSessionFactory().openSession();
                try {

                    Criteria criteria = session.createCriteria(User.class);
                    criteria.add(Restrictions.eq("mobile", mobile));
                    criteria.add(Restrictions.eq("password", password));

                    User users = (User) criteria.uniqueResult();

                    if (users == null) {
                        responseJson.addProperty("message", "Invalid details");
                    } else {
                        responseJson.addProperty("status", true);
                        responseJson.add("user", gson.toJsonTree(users));
                    }
                } finally {
                    session.close();
                }
            }
        } catch (Exception e) {
            responseJson.addProperty("message", "Invalid JSON format");
        }
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));
    }
}
