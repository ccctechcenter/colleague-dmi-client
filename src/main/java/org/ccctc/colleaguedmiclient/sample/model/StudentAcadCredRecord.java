package org.ccctc.colleaguedmiclient.sample.model;

import lombok.Getter;
import lombok.Setter;
import org.ccctc.colleaguedmiclient.annotation.Association;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Field;
import org.ccctc.colleaguedmiclient.annotation.Ignore;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * This is a sample Entity to read from STUDENT.ACAD.CRED as well as the following child records (which in turn
 * also have child records):
 *
 * PERSON
 * COURSE
 * TERM
 * STUDENT.COURSE.SEC
 *
 * Field names are auto-mapped by converting camel case to Colleague field name format. ie stcCourseName = STC.COURSE.NAME.
 */
@Getter
@Setter
@Entity(appl = "ST", name = "STUDENT.ACAD.CRED")
public class StudentAcadCredRecord extends ColleagueRecord {

    // string fields
    String stcCourseName;
    String stcSectionNo;
    String stcReplCode;
    String stcPrintedComments;

    // example of overriding the field name
    @Field(value = "STC.TITLE")
    String title;

    // example of a field that you want ignored
    @Ignore
    String ignoreMe;


    //
    // Numeric types: If they contain decimal information they must be BigDecimal
    // NOTE: if there is no decimal component, the data type must be Integer or Long depending on size of field (9 digits
    // or less will be Integer, otherwise Long).
    //
    BigDecimal stcCred;
    BigDecimal stcCmplCred;
    BigDecimal stcAltcumContribCmplCred;
    BigDecimal stcAltcumContribGradePts;
    BigDecimal stcAltcumContribGpaCred;


    // date type fields (note: time fields use LocalTime)
    LocalDate stcStartDate;
    LocalDate stcEndDate;


    // TIP: Load the first value of a multi-valued field by using a non-array data type
    @Field(value = "STC.STATUS")
    String currentStcStatus;

    // Association - must be List<>
    @Association
    List<StcStatusesAssoc> stcStatusesAssoc;

    // Typical single-valued join
    @Join
    CoursesRecord stcCourse;

    // Single-valued join with custom field name
    @Join(value = "STC.TERM")
    TermsRecord term;

    @Join
    StudentCourseSecRecord stcStudentCourseSec;

    @Join
    GradesRecord stcVerifiedGrade;

    @Join
    PersonRecord stcPersonId;

    // join multiple times on a single field
    @Join(value = "STC.PERSON.ID")
    StudentsRecord student;

}
