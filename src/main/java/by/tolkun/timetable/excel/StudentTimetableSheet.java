package by.tolkun.timetable.excel;

import by.tolkun.timetable.config.StudentTimetableConfig;
import by.tolkun.timetable.entity.SchoolClass;
import by.tolkun.timetable.entity.SchoolDay;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class to represent student timetable like a excel sheet.
 */
public class StudentTimetableSheet {
    private Sheet sheet;

    /**
     * Constructor with parameters.
     *
     * @param sheet of the student timetable
     */
    public StudentTimetableSheet(Sheet sheet) {
        this.sheet = sheet;
    }

    /**
     * Get sheet of student timetable.
     *
     * @return sheet of the student timetable
     */
    public Sheet getSheet() {
        return sheet;
    }

    /**
     * Set sheet of student timetable.
     *
     * @param sheet of the student timetable in the excel workbook
     */
    public void setSheet(Sheet sheet) {
        this.sheet = sheet;
    }

    /**
     * Get physical number of the rows in a sheet.
     *
     * @return physical number of the rows in a sheet
     */
    public int getPhysicalNumberOfRows() {
        return sheet.getPhysicalNumberOfRows();
    }

    /**
     * Get physical number of the columns in a sheet.
     *
     * @return physical number of the columns in a sheet
     */
    public int getPhysicalNumberOfColumns() {
        int maxNumCells = 0;
        for (Row row : sheet) {
            if (maxNumCells < row.getLastCellNum()) {
                maxNumCells = row.getLastCellNum();
            }
        }
        return maxNumCells;
    }

    /**
     * Get quantity of the lessons per day according to shift and class.
     *
     * @param shift       of the day of a class
     * @param schoolDay   the school day
     * @param schoolClass the school class
     * @return quantity of the lessons per day according to shift and class
     */
    public int getQtyLessonsByShiftAndDayAndClass(int shift, int schoolDay,
                                                  int schoolClass) {
        int currentShiftBeginRow
                = StudentTimetableConfig.NUM_OF_FIRST_ROW_WITH_LESSON
                + schoolDay * StudentTimetableConfig.LESSONS_PER_DAY;
        int currentShiftEndRow = currentShiftBeginRow
                + StudentTimetableConfig.QTY_LESSONS_PER_FIRST_SHIFT;

        if (shift == 2) {
            currentShiftBeginRow = currentShiftEndRow;
            currentShiftEndRow = currentShiftBeginRow
                    + StudentTimetableConfig.QTY_LESSONS_PER_SECOND_SHIFT;
        }

        int qtyLessonPerCurrentShift = 0;
        for (int i = currentShiftBeginRow; i < currentShiftEndRow; i++) {
            if (!sheet
                    .getRow(i)
                    .getCell(schoolClass)
                    .getStringCellValue()
                    .isEmpty()) {
                qtyLessonPerCurrentShift++;
            }
        }

        return qtyLessonPerCurrentShift;
    }

    /**
     * Get shift by day and class.
     *
     * @param schoolDay   the school day
     * @param schoolClass the school class
     * @return shift according to day and class
     */
    public int getShiftByDayAndClass(int schoolDay, int schoolClass) {
        if (getQtyLessonsByShiftAndDayAndClass(1, schoolDay, schoolClass)
                > getQtyLessonsByShiftAndDayAndClass(2, schoolDay,
                schoolClass)) {
            return 1;
        }
        return 2;
    }

    /**
     * Get list of the lessons by day and class.
     *
     * @param schoolDay   the school day
     * @param schoolClass the school class
     * @return list of the lessons according to day and class
     */
    public List<String> getLessonsByDayAndClass(int schoolDay, int schoolClass) {
        int shift = getShiftByDayAndClass(schoolDay, schoolClass);
        int qtyLessonsPerCurrentShift
                = StudentTimetableConfig.QTY_LESSONS_PER_FIRST_SHIFT;
        if (shift == 2) {
            qtyLessonsPerCurrentShift
                    = StudentTimetableConfig.QTY_LESSONS_PER_SECOND_SHIFT;
        }
        int numOfFirstLessonForCurrentDayAndShift
                = StudentTimetableConfig.NUM_OF_FIRST_ROW_WITH_LESSON
                + schoolDay * StudentTimetableConfig.LESSONS_PER_DAY
                + (shift - 1) * StudentTimetableConfig.QTY_LESSONS_PER_FIRST_SHIFT;

        List<String> lessons = new ArrayList<>();

//        Read all lessons with tilings the window.
        for (int i = numOfFirstLessonForCurrentDayAndShift;
             i < numOfFirstLessonForCurrentDayAndShift
                     + qtyLessonsPerCurrentShift; i++) {
            lessons.add(sheet
                    .getRow(i)
                    .getCell(schoolClass)
                    .getStringCellValue()
                    .trim()
            );
        }

//        Remove empty string from the end of the list
//        tilings the windows will remain.
        int currentLesson = lessons.size() - 1;
        while (currentLesson >= 0 && lessons.get(currentLesson).isEmpty()) {
            currentLesson--;
        }
        lessons = lessons.subList(0, currentLesson + 1);

//        Add lessons that are before begin of second shift.
        if (shift == 2) {
            for (int i = numOfFirstLessonForCurrentDayAndShift - 1;
                 i > numOfFirstLessonForCurrentDayAndShift - 1
                         - StudentTimetableConfig.QTY_LESSONS_PER_FIRST_SHIFT;
                 i--) {
                String lesson = sheet
                        .getRow(i)
                        .getCell(schoolClass)
                        .getStringCellValue()
                        .trim();
                if (!lesson.isEmpty()) {
                    lessons.add("!" + lesson);
                }
            }
        }

        return lessons;
    }

    /**
     * Get the list of a school classes.
     *
     * @return get the list of a school classes
     */
    public List<SchoolClass> getSchoolClasses() {
        List<SchoolClass> schoolClasses = new ArrayList<>();

//        Loop by classes to get list of SchoolClasses.
        for (int numOfCurrentClass = StudentTimetableConfig
                .NUM_OF_FIRST_COLUMN_WITH_LESSON;
             numOfCurrentClass < getPhysicalNumberOfColumns();
             numOfCurrentClass++) {

            List<SchoolDay> schoolDays = new ArrayList<>();

//            Loop by days to get list of SchoolDays.
            for (int numOfCurrentDay = 0;
                 numOfCurrentDay < StudentTimetableConfig.QTY_SCHOOL_DAYS_PER_WEEK;
                 numOfCurrentDay++) {

                int shift = getShiftByDayAndClass(numOfCurrentDay,
                        numOfCurrentClass);
                schoolDays.add(new SchoolDay(getLessonsByDayAndClass(
                        numOfCurrentDay, numOfCurrentClass), shift)
                );
            }

            schoolClasses.add(new SchoolClass(sheet
                    .getRow(StudentTimetableConfig
                            .NUM_OF_FIRST_ROW_WITH_LESSON - 1)
                    .getCell(numOfCurrentClass)
                    .getStringCellValue(), schoolDays)
            );
        }

        return schoolClasses;
    }

    /**
     * Set autosize of all columns.
     */
    public void autoSizeAllColumns() {
        for (int i = 0; i < getPhysicalNumberOfColumns(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * Clear all rows.
     */
    public void clearAll() {
        sheet.shiftRows(getPhysicalNumberOfRows(), getPhysicalNumberOfRows() * 2,
                -(getPhysicalNumberOfRows()));
    }

    /**
     * Compares this StudentTimeTableSheet to the specified object. The result
     * is true if and only if the argument is not null and is a String object
     * that represents the same sequence of characters as this object.
     *
     * @param o the object to compare this String against
     * @return true if the given object represents a String equivalent to this
     * string, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentTimetableSheet)) return false;
        StudentTimetableSheet that = (StudentTimetableSheet) o;
        return Objects.equals(sheet, that.sheet);
    }

    /**
     * Returns a hash code for this StudentTimetableSheet.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(sheet);
    }

    /**
     * Returns the string representation of the StudentTimetableSheet.
     *
     * @return the string representation of the StudentTimetableSheet
     */
    @Override
    public String toString() {
        return "TimetableExcelWorkbook{" +
                "sheet=" + sheet +
                '}';
    }
}