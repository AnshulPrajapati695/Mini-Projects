
from flask import Flask, render_template, request, redirect, url_for, flash
import sqlite3
from pathlib import Path
from datetime import date

app = Flask(__name__)
app.secret_key = "expense-tracker-local-key"

# Keep the database in the project folder
BASE_DIR = Path(__file__).resolve().parent
DATABASE = BASE_DIR / "expenses.db"

CATEGORIES = [
    "Food",
    "Transport",
    "Shopping",
    "Education",
    "Bills",
    "Entertainment",
    "Health",
    "Other"
]


def get_db_connection():
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row
    return conn


def initialize_database():
    conn = get_db_connection()

    conn.execute("""
        CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            amount REAL NOT NULL CHECK(amount > 0),
            category TEXT NOT NULL,
            expense_date TEXT NOT NULL,
            description TEXT
        )
    """)

    conn.commit()
    conn.close()


@app.route("/")
def index():
    conn = get_db_connection()

    expenses = conn.execute("""
        SELECT * FROM expenses
        ORDER BY expense_date DESC, id DESC
    """).fetchall()

    result = conn.execute("""
        SELECT
            COALESCE(SUM(amount), 0) AS total,
            COUNT(*) AS count
        FROM expenses
    """).fetchone()

    category_totals = conn.execute("""
        SELECT category, SUM(amount) AS total
        FROM expenses
        GROUP BY category
        ORDER BY total DESC
    """).fetchall()

    conn.close()

    return render_template(
        "index.html",
        expenses=expenses,
        total=result["total"],
        count=result["count"],
        category_totals=category_totals
    )


@app.route("/add", methods=["GET", "POST"])
def add_expense():
    if request.method == "POST":
        title = request.form.get("title", "").strip()
        amount_text = request.form.get("amount", "").strip()
        category = request.form.get("category", "")
        expense_date = request.form.get("expense_date", "")
        description = request.form.get("description", "").strip()

        # Validate the form
        try:
            amount = float(amount_text)
        except ValueError:
            amount = 0

        if not title or len(title) > 100:
            flash("Enter a title (maximum 100 characters).", "error")
            return redirect(url_for("add_expense"))

        if amount <= 0:
            flash("Amount must be greater than zero.", "error")
            return redirect(url_for("add_expense"))

        if category not in CATEGORIES:
            flash("Please select a valid category.", "error")
            return redirect(url_for("add_expense"))

        try:
            parsed_date = date.fromisoformat(expense_date)
            if parsed_date > date.today():
                raise ValueError
        except ValueError:
            flash("Enter a valid date that is not in the future.", "error")
            return redirect(url_for("add_expense"))

        if len(description) > 500:
            flash("Description must be 500 characters or less.", "error")
            return redirect(url_for("add_expense"))

        conn = get_db_connection()

        conn.execute("""
            INSERT INTO expenses
            (title, amount, category, expense_date, description)
            VALUES (?, ?, ?, ?, ?)
        """, (title, amount, category, expense_date, description))

        conn.commit()
        conn.close()

        flash("Expense added successfully!", "success")
        return redirect(url_for("index"))

    return render_template(
        "add_expense.html",
        categories=CATEGORIES,
        today=date.today().isoformat()
    )


@app.route("/delete/<int:expense_id>", methods=["POST"])
def delete_expense(expense_id):
    conn = get_db_connection()

    conn.execute(
        "DELETE FROM expenses WHERE id = ?",
        (expense_id,)
    )

    conn.commit()
    conn.close()

    flash("Expense deleted successfully.", "success")
    return redirect(url_for("index"))


# Create the database and table automatically
initialize_database()

if __name__ == "__main__":
    app.run(debug=True)
