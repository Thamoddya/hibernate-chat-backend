package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import entity.User;
import entity.User_Status;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.HibernateUtil;
import model.Validation;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

@MultipartConfig
@WebServlet(name = "SignUp", urlPatterns = {"/SignUp"})
public class SignUp extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Gson gson = new Gson();
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("status", false);

        String mobile = req.getParameter("mobile");
        String firstName = req.getParameter("firstName");
        String lastName = req.getParameter("lastName");
        String password = req.getParameter("password");
        Part imagePart = req.getPart("avatar-image");

        if (mobile.isEmpty()) {
            responseJson.addProperty("message", "Please Enter mobile Number");
        } else if (!Validation.isMobileNumberValid(mobile)) {
            responseJson.addProperty("message", "Invalid Mobile Number");
        } else if (firstName.isEmpty()) {
            responseJson.addProperty("message", "Please Enter your First Name");
        } else if (lastName.isEmpty()) {
            responseJson.addProperty("message", "Please Enter your Last Name");
        } else if (password.isEmpty()) {
            responseJson.addProperty("message", "Please Enter your Password");
        } else if (!Validation.isPasswordValid(password)) {
            responseJson.addProperty("message", "Password must be at least 8 characters and include "
                    + "a capital letter, lowercase letter, numbers, and a special character.");
        } else {
            Session session = HibernateUtil.getSessionFactory().openSession();

            Criteria criteria = session.createCriteria(User.class);
            criteria.add(Restrictions.eq("mobile", mobile));

            if (!criteria.list().isEmpty()) {
                responseJson.addProperty("message", "Mobile Number already Used.");
            } else {
                try {
                    session.beginTransaction();

                    User user = new User();
                    user.setFirst_name(firstName);
                    user.setLast_name(lastName);
                    user.setMobile(mobile);
                    user.setPassword(password);
                    user.setRegister_date_time(new Date());

                    User_Status user_Status = (User_Status) session.get(User_Status.class, 2);
                    user.setUser_Status(user_Status);

                    session.save(user);
                    session.getTransaction().commit();

                    if (imagePart != null && imagePart.getSize() > 0) {
                        String applicationPath = req.getServletContext().getRealPath("");
                        String newApplicationPath = applicationPath.replace("\\build\\web", "\\web");
                        File avatarDir = new File(newApplicationPath + File.separator + "avatarImages");

                        if (!avatarDir.exists()) {
                            avatarDir.mkdirs();
                        }

                        String avatarImagePath = avatarDir + File.separator + mobile + ".jpg";
                        File file = new File(avatarImagePath);
                        Files.copy(imagePart.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Avatar image uploaded to: " + avatarImagePath);
                    }

                    responseJson.addProperty("status", true);
                    responseJson.addProperty("message", "Register Completed");
                } catch (Exception e) {
                    e.printStackTrace();
                    responseJson.addProperty("message", "Error occurred during registration");
                } finally {
                    session.close();
                }
            }
        }
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(responseJson));
    }

}
