-- =====================================================
-- HOSPITAL APPOINTMENT SYSTEM  (PL/SQL backend)
-- Run on: Oracle Live SQL (livesql.oracle.com) or FreeSQL
-- Paste the WHOLE file in the editor and click Run.
-- =====================================================

-- 1. Clean start (safe to re-run)
BEGIN
  FOR t IN (SELECT table_name FROM user_tables
            WHERE table_name IN ('APPOINTMENTS','PATIENTS','DOCTORS')) LOOP
    EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
  END LOOP;
END;
/

-- 2. Tables
CREATE TABLE doctors (
  doctor_id      NUMBER PRIMARY KEY,
  name           VARCHAR2(60) NOT NULL,
  specialization VARCHAR2(40) NOT NULL,
  fee            NUMBER(8,2)  NOT NULL
);

CREATE TABLE patients (
  patient_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name       VARCHAR2(60) NOT NULL,
  phone      VARCHAR2(15) NOT NULL UNIQUE
);

CREATE TABLE appointments (
  appt_id    NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  patient_id NUMBER NOT NULL REFERENCES patients(patient_id),
  doctor_id  NUMBER NOT NULL REFERENCES doctors(doctor_id),
  appt_date  DATE   NOT NULL,
  slot       VARCHAR2(5) NOT NULL,
  status     VARCHAR2(10) DEFAULT 'BOOKED'
             CHECK (status IN ('BOOKED','CANCELLED','COMPLETED')),
  created_at TIMESTAMP DEFAULT SYSTIMESTAMP
);

-- 3. Doctors (same list as the HTML UI)
INSERT INTO doctors VALUES (1, 'Dr. Anita Sharma', 'Cardiology',  800);
INSERT INTO doctors VALUES (2, 'Dr. Rohan Mehta',  'Orthopedics', 700);
INSERT INTO doctors VALUES (3, 'Dr. Simran Kaur',  'Pediatrics',  500);
INSERT INTO doctors VALUES (4, 'Dr. Vikram Singh', 'General',     300);
COMMIT;

-- 4. Handy view for reports
CREATE OR REPLACE VIEW v_appointments AS
SELECT a.appt_id, p.name AS patient, p.phone, d.name AS doctor,
       d.specialization, a.appt_date, a.slot, a.status
FROM   appointments a
JOIN   patients p ON p.patient_id = a.patient_id
JOIN   doctors  d ON d.doctor_id  = a.doctor_id;

-- 5. Package specification
CREATE OR REPLACE PACKAGE hospital_pkg AS
  PROCEDURE book_appointment(p_name VARCHAR2, p_phone VARCHAR2,
                             p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2);
  PROCEDURE cancel_appointment(p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2);
  PROCEDURE complete_appointment(p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2);
  PROCEDURE show_appointments;
END hospital_pkg;
/

-- 6. Package body
CREATE OR REPLACE PACKAGE BODY hospital_pkg AS

  PROCEDURE book_appointment(p_name VARCHAR2, p_phone VARCHAR2,
                             p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2) IS
    v_pid patients.patient_id%TYPE;
    v_cnt NUMBER;
  BEGIN
    SELECT COUNT(*) INTO v_cnt FROM doctors WHERE doctor_id = p_doctor_id;
    IF v_cnt = 0 THEN
      RAISE_APPLICATION_ERROR(-20002, 'Doctor not found');
    END IF;

    -- block double booking
    SELECT COUNT(*) INTO v_cnt FROM appointments
    WHERE  doctor_id = p_doctor_id AND appt_date = TRUNC(p_date)
    AND    slot = p_slot AND status = 'BOOKED';
    IF v_cnt > 0 THEN
      RAISE_APPLICATION_ERROR(-20001, 'Slot already booked for this doctor');
    END IF;

    -- reuse patient if phone already exists
    BEGIN
      SELECT patient_id INTO v_pid FROM patients WHERE phone = p_phone;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      INSERT INTO patients(name, phone) VALUES (p_name, p_phone)
      RETURNING patient_id INTO v_pid;
    END;

    INSERT INTO appointments(patient_id, doctor_id, appt_date, slot)
    VALUES (v_pid, p_doctor_id, TRUNC(p_date), p_slot);
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Booked: ' || p_name || ' | ' ||
                         TO_CHAR(p_date,'DD-Mon-YYYY') || ' ' || p_slot);
  END book_appointment;

  -- private helper: change status of a BOOKED appointment
  PROCEDURE set_status(p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2, p_new VARCHAR2) IS
  BEGIN
    UPDATE appointments SET status = p_new
    WHERE  doctor_id = p_doctor_id AND appt_date = TRUNC(p_date)
    AND    slot = p_slot AND status = 'BOOKED';
    IF SQL%ROWCOUNT = 0 THEN
      RAISE_APPLICATION_ERROR(-20004, 'No active appointment found');
    END IF;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Appointment ' || p_new || ': ' ||
                         TO_CHAR(p_date,'DD-Mon-YYYY') || ' ' || p_slot);
  END set_status;

  PROCEDURE cancel_appointment(p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2) IS
  BEGIN
    set_status(p_doctor_id, p_date, p_slot, 'CANCELLED');
  END;

  PROCEDURE complete_appointment(p_doctor_id NUMBER, p_date DATE, p_slot VARCHAR2) IS
  BEGIN
    set_status(p_doctor_id, p_date, p_slot, 'COMPLETED');
  END;

  PROCEDURE show_appointments IS
  BEGIN
    FOR r IN (SELECT * FROM v_appointments ORDER BY appt_date, slot) LOOP
      DBMS_OUTPUT.PUT_LINE(r.appt_id || ' | ' || r.patient || ' | ' || r.doctor ||
                           ' | ' || TO_CHAR(r.appt_date,'DD-Mon-YYYY') ||
                           ' ' || r.slot || ' | ' || r.status);
    END LOOP;
  END show_appointments;

END hospital_pkg;
/

-- 7. Quick test (you can delete this part)
BEGIN
  hospital_pkg.book_appointment('Amit Verma', '9876500001', 1, DATE '2026-11-02', '10:00');
  hospital_pkg.book_appointment('Neha Gupta', '9876500002', 3, DATE '2026-11-02', '11:00');
  hospital_pkg.cancel_appointment(3, DATE '2026-11-02', '11:00');
  hospital_pkg.show_appointments;
END;
/

SELECT * FROM v_appointments ORDER BY appt_id;
