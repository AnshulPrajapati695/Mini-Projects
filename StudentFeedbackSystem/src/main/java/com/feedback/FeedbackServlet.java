package com.feedback;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * FeedbackServlet: handles all feedback operations at the URL /feedback.
 *  - POST action=add    : insert a new feedback record      (Step 8)
 *  - GET  action=list   : show all records                  (Step 9)
 *  - GET  action=edit   : load one record into a form       (Step 10)
 *  - POST action=update : save edited record                (Step 10)
 *  - POST action=delete : delete a record                   (Step 11)
 */
@WebServlet("/feedback")
public class FeedbackServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Faculty names that are allowed (must match the dropdown in index.html)
    private static final List<String> VALID_FACULTIES =
            Arrays.asList("Faculty A", "Faculty B", "Faculty C", "Faculty D", "Faculty E");

    // Rating labels: index 0 = rating 1, index 4 = rating 5
    private static final String[] RATING_LABELS =
            {"Very Bad", "Bad", "Good", "Very Good", "Excellent"};

    // Date format shown in the table
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ---------------------------------------------------------------
    // GET: read-only operations (no validation needed)
    // ---------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String action = trim(request.getParameter("action"));

        if (action.isEmpty() || "list".equals(action)) {
            showList(request, response);
        } else if ("edit".equals(action)) {
            showEditForm(request, response);
        } else {
            redirectWithError(request, response, "Unknown action.");
        }
    }

    // ---------------------------------------------------------------
    // POST: operations that change data
    // ---------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Must be set BEFORE reading any parameter, so special characters work
        request.setCharacterEncoding("UTF-8");

        String action = trim(request.getParameter("action"));

        if ("add".equals(action)) {
            addFeedback(request, response);
        } else if ("update".equals(action)) {
            updateFeedback(request, response);
        } else if ("delete".equals(action)) {
            deleteFeedback(request, response);
        } else {
            redirectWithError(request, response, "Unknown action.");
        }
    }

    // ---------------------------------------------------------------
    // LIST: SELECT all records and display them in an HTML table
    // ---------------------------------------------------------------
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        List<Feedback> list = new ArrayList<>();
        String dbError = null;

        String sql = "SELECT id, student_name, faculty, rating, comments, created_at "
                   + "FROM feedback ORDER BY id ASC";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(readRow(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();   // full details in the Eclipse Console
            dbError = "Database error: could not load feedback records.";
        }

        // Messages passed from other operations (update/delete) via the URL
        String status = trim(request.getParameter("status"));
        String error = trim(request.getParameter("error"));

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String ctx = request.getContextPath();

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\"><head>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<title>All Feedback</title>");
        out.println("<link rel=\"stylesheet\" href=\"" + ctx + "/css/style.css\">");
        out.println("</head><body>");
        out.println("<div class=\"container wide\">");
        out.println("<h1>All Feedback</h1>");

        // Messages
        if (dbError != null) {
            out.println("<div class=\"message error\">" + escape(dbError) + "</div>");
        } else if ("updated".equals(status)) {
            out.println("<div class=\"message success\">Feedback updated successfully.</div>");
        } else if ("deleted".equals(status)) {
            out.println("<div class=\"message success\">Feedback deleted successfully.</div>");
        }
        if (!error.isEmpty()) {
            out.println("<div class=\"message error\">" + escape(error) + "</div>");
        }

        out.println("<a href=\"" + ctx + "/index.html\" class=\"btn\">+ Add New Feedback</a>");

        if (dbError == null && list.isEmpty()) {
            out.println("<p class=\"empty-message\">No feedback records found yet. "
                      + "Click \"Add New Feedback\" to submit the first one.</p>");
        } else if (dbError == null) {
            out.println("<div class=\"table-wrapper\">");
            out.println("<table>");
            out.println("<tr><th>ID</th><th>Student Name</th><th>Faculty</th>"
                      + "<th>Rating</th><th>Comments</th><th>Submitted On</th>"
                      + "<th>Actions</th></tr>");

            for (Feedback f : list) {
                String comments = (f.getComments() == null || f.getComments().isEmpty())
                        ? "-" : escape(f.getComments());
                String date = (f.getCreatedAt() == null)
                        ? "-" : f.getCreatedAt().toLocalDateTime().format(DATE_FORMAT);

                out.println("<tr>");
                out.println("<td>" + f.getId() + "</td>");
                out.println("<td>" + escape(f.getStudentName()) + "</td>");
                out.println("<td>" + escape(f.getFaculty()) + "</td>");
                out.println("<td>" + escape(f.getRatingText()) + "</td>");
                out.println("<td>" + comments + "</td>");
                out.println("<td>" + date + "</td>");
                out.println("<td>");
                // Edit = GET
                out.println("<a class=\"btn btn-edit\" href=\"" + ctx
                          + "/feedback?action=edit&id=" + f.getId() + "\">Edit</a> ");
                // Delete = POST with a confirmation popup
                out.println("<form class=\"inline-form\" action=\"" + ctx
                          + "/feedback\" method=\"post\" "
                          + "onsubmit=\"return confirm('Are you sure you want to delete this feedback?');\">");
                out.println("<input type=\"hidden\" name=\"action\" value=\"delete\">");
                out.println("<input type=\"hidden\" name=\"id\" value=\"" + f.getId() + "\">");
                out.println("<button type=\"submit\" class=\"btn btn-delete\">Delete</button>");
                out.println("</form>");
                out.println("</td>");
                out.println("</tr>");
            }

            out.println("</table></div>");
        }

        out.println("</div></body></html>");
    }

    // ---------------------------------------------------------------
    // EDIT (GET): load ONE record and show it in a pre-filled form
    // ---------------------------------------------------------------
    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id;
        try {
            id = Integer.parseInt(trim(request.getParameter("id")));
        } catch (NumberFormatException e) {
            redirectToListWithError(request, response, "Invalid feedback ID.");
            return;
        }

        Feedback f = null;
        String sql = "SELECT id, student_name, faculty, rating, comments, created_at "
                   + "FROM feedback WHERE id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f = readRow(rs);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            redirectToListWithError(request, response,
                    "Database error: could not load the feedback record.");
            return;
        }

        if (f == null) {
            redirectToListWithError(request, response,
                    "Feedback record with ID " + id + " was not found.");
            return;
        }

        String error = trim(request.getParameter("error"));
        String ctx = request.getContextPath();

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\"><head>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("<title>Edit Feedback</title>");
        out.println("<link rel=\"stylesheet\" href=\"" + ctx + "/css/style.css\">");
        out.println("</head><body>");
        out.println("<div class=\"container\">");
        out.println("<h1>Edit Feedback #" + f.getId() + "</h1>");

        if (!error.isEmpty()) {
            out.println("<div class=\"message error\">" + escape(error) + "</div>");
        }

        out.println("<form action=\"" + ctx + "/feedback\" method=\"post\">");
        out.println("<input type=\"hidden\" name=\"action\" value=\"update\">");
        out.println("<input type=\"hidden\" name=\"id\" value=\"" + f.getId() + "\">");

        // Student name
        out.println("<label for=\"studentName\">Student Name <span class=\"required\">*</span></label>");
        out.println("<input type=\"text\" id=\"studentName\" name=\"studentName\" maxlength=\"80\" "
                  + "value=\"" + escape(f.getStudentName()) + "\" required>");

        // Faculty dropdown: the saved faculty is pre-selected
        out.println("<label for=\"faculty\">Faculty <span class=\"required\">*</span></label>");
        out.println("<select id=\"faculty\" name=\"faculty\" required>");
        for (String fac : VALID_FACULTIES) {
            String selected = fac.equals(f.getFaculty()) ? " selected" : "";
            out.println("<option value=\"" + escape(fac) + "\"" + selected + ">"
                      + escape(fac) + "</option>");
        }
        out.println("</select>");

        // Rating radio buttons: the saved rating is pre-checked
        out.println("<label>Rating <span class=\"required\">*</span></label>");
        out.println("<div class=\"radio-group\">");
        for (int i = 0; i < RATING_LABELS.length; i++) {
            int value = i + 1;
            String checked = (value == f.getRating()) ? " checked" : "";
            out.println("<label><input type=\"radio\" name=\"rating\" value=\"" + value + "\""
                      + checked + " required> " + RATING_LABELS[i] + "</label>");
        }
        out.println("</div>");

        // Comments
        String comments = (f.getComments() == null) ? "" : f.getComments();
        out.println("<label for=\"comments\">Comments <span class=\"hint\">(optional, max 1000 characters)</span></label>");
        out.println("<textarea id=\"comments\" name=\"comments\" maxlength=\"1000\">"
                  + escape(comments) + "</textarea>");

        out.println("<div class=\"button-row\">");
        out.println("<button type=\"submit\" class=\"btn\">Update Feedback</button>");
        out.println("<a href=\"" + ctx + "/feedback?action=list\" class=\"btn btn-secondary\">Cancel</a>");
        out.println("</div>");
        out.println("</form>");
        out.println("</div></body></html>");
    }

    // ---------------------------------------------------------------
    // ADD (POST): validate, then INSERT
    // ---------------------------------------------------------------
    private void addFeedback(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String studentName = trim(request.getParameter("studentName"));
        String faculty = trim(request.getParameter("faculty"));
        String ratingText = trim(request.getParameter("rating"));
        String comments = trim(request.getParameter("comments"));

        String validationError = validate(studentName, faculty, ratingText, comments);
        if (validationError != null) {
            redirectWithError(request, response, validationError);
            return;
        }
        int rating = Integer.parseInt(ratingText);   // already checked by validate()

        String sql = "INSERT INTO feedback (student_name, faculty, rating, comments) "
                   + "VALUES (?, ?, ?, ?)";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, studentName);
            ps.setString(2, faculty);
            ps.setInt(3, rating);

            if (comments.isEmpty()) {
                ps.setNull(4, java.sql.Types.VARCHAR);   // optional field: store NULL
            } else {
                ps.setString(4, comments);
            }

            int rows = ps.executeUpdate();

            if (rows == 1) {
                response.sendRedirect(request.getContextPath() + "/index.html?status=added");
            } else {
                redirectWithError(request, response, "Feedback could not be saved. Please try again.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            redirectWithError(request, response,
                    "Database error: feedback was NOT saved. Please check the database connection.");
        }
    }

    // ---------------------------------------------------------------
    // UPDATE (POST): validate, then UPDATE ... WHERE id = ?
    // ---------------------------------------------------------------
    private void updateFeedback(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id;
        try {
            id = Integer.parseInt(trim(request.getParameter("id")));
        } catch (NumberFormatException e) {
            redirectToListWithError(request, response, "Invalid feedback ID.");
            return;
        }

        String studentName = trim(request.getParameter("studentName"));
        String faculty = trim(request.getParameter("faculty"));
        String ratingText = trim(request.getParameter("rating"));
        String comments = trim(request.getParameter("comments"));

        // If invalid, go back to the edit form of the SAME record with a message
        String validationError = validate(studentName, faculty, ratingText, comments);
        if (validationError != null) {
            redirectToEditWithError(request, response, id, validationError);
            return;
        }
        int rating = Integer.parseInt(ratingText);

        String sql = "UPDATE feedback SET student_name = ?, faculty = ?, rating = ?, comments = ? "
                   + "WHERE id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, studentName);
            ps.setString(2, faculty);
            ps.setInt(3, rating);

            if (comments.isEmpty()) {
                ps.setNull(4, java.sql.Types.VARCHAR);
            } else {
                ps.setString(4, comments);
            }

            ps.setInt(5, id);

            int rows = ps.executeUpdate();

            if (rows == 1) {
                // Post/Redirect/Get: list page shows the success message
                response.sendRedirect(request.getContextPath()
                        + "/feedback?action=list&status=updated");
            } else {
                // 0 rows = no record with this id (maybe it was deleted meanwhile)
                redirectToListWithError(request, response,
                        "Update failed: feedback record with ID " + id + " was not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            redirectToEditWithError(request, response, id,
                    "Database error: feedback was NOT updated. Please check the database connection.");
        }
    }

    // ---------------------------------------------------------------
    // DELETE (POST): DELETE FROM feedback WHERE id = ?
    // ---------------------------------------------------------------
    private void deleteFeedback(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int id;
        try {
            id = Integer.parseInt(trim(request.getParameter("id")));
        } catch (NumberFormatException e) {
            redirectToListWithError(request, response, "Invalid feedback ID.");
            return;
        }

        String sql = "DELETE FROM feedback WHERE id = ?";

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            int rows = ps.executeUpdate();   // number of rows deleted

            if (rows == 1) {
                // Post/Redirect/Get: the list reloads from the database
                response.sendRedirect(request.getContextPath()
                        + "/feedback?action=list&status=deleted");
            } else {
                // 0 rows = nothing deleted (record already gone)
                redirectToListWithError(request, response,
                        "Delete failed: feedback record with ID " + id + " was not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            redirectToListWithError(request, response,
                    "Database error: feedback was NOT deleted. Please check the database connection.");
        }
    }

    // ---------------------------------------------------------------
    // Helper methods
    // ---------------------------------------------------------------

    /**
     * Checks the form values. Returns an error message,
     * or null if everything is valid. Used by both Add and Update.
     */
    private String validate(String studentName, String faculty, String ratingText, String comments) {
        if (studentName.isEmpty()) {
            return "Student name is required.";
        }
        if (studentName.length() > 80) {
            return "Student name must be 80 characters or fewer.";
        }
        if (!VALID_FACULTIES.contains(faculty)) {
            return "Please select a valid faculty.";
        }

        int rating;
        try {
            rating = Integer.parseInt(ratingText);
        } catch (NumberFormatException e) {
            return "Please select a rating.";
        }
        if (rating < 1 || rating > 5) {
            return "Rating must be between Very Bad and Excellent.";
        }
        if (comments.length() > 1000) {
            return "Comments must be 1000 characters or fewer.";
        }
        return null;   // valid
    }

    /** Converts the current row of a ResultSet into a Feedback object. */
    private Feedback readRow(ResultSet rs) throws SQLException {
        Feedback f = new Feedback();
        f.setId(rs.getInt("id"));
        f.setStudentName(rs.getString("student_name"));
        f.setFaculty(rs.getString("faculty"));
        f.setRating(rs.getInt("rating"));
        f.setComments(rs.getString("comments"));   // may be null
        f.setCreatedAt(rs.getTimestamp("created_at"));
        return f;
    }

    /** Returns "" if the value is null, otherwise the trimmed value. */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Makes text safe to print inside HTML.
     * Without this, a comment like <script>...</script> would run in the browser (XSS).
     */
    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /** Redirects to the home page with an error message. */
    private void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                   String message) throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/index.html?error=" + encoded);
    }

    /** Redirects to the feedback list with an error message. */
    private void redirectToListWithError(HttpServletRequest request, HttpServletResponse response,
                                         String message) throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/feedback?action=list&error=" + encoded);
    }

    /** Redirects back to the edit form of one record with an error message. */
    private void redirectToEditWithError(HttpServletRequest request, HttpServletResponse response,
                                         int id, String message) throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath()
                + "/feedback?action=edit&id=" + id + "&error=" + encoded);
    }
}