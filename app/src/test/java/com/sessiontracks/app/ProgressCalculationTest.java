package com.sessiontracks.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sessiontracks.app.data.entity.StudySession;
import com.sessiontracks.app.data.model.ChapterProgress;
import com.sessiontracks.app.data.model.OverallStats;
import com.sessiontracks.app.data.model.SubjectProgress;

import org.junit.Test;

/** Unit tests for percentage maths and session duration handling. */
public class ProgressCalculationTest {

    @Test
    public void chapterProgress_computesPercentage() {
        assertEquals(60, new ChapterProgress("c", 20, 12).getPercent());
        assertEquals(100, new ChapterProgress("c", 5, 5).getPercent());
        assertEquals(0, new ChapterProgress("c", 5, 0).getPercent());
    }

    @Test
    public void chapterProgress_handlesEmptyChapterWithoutDivideByZero() {
        ChapterProgress progress = new ChapterProgress("c", 0, 0);
        assertEquals(0, progress.getPercent());
        assertFalse(progress.isComplete());
    }

    @Test
    public void chapterProgress_reportsCompletion() {
        assertTrue(new ChapterProgress("c", 3, 3).isComplete());
        assertFalse(new ChapterProgress("c", 3, 2).isComplete());
    }

    @Test
    public void subjectAndOverallProgress_computePercentages() {
        assertEquals(50, new SubjectProgress("s", 10, 5, 2).getPercent());
        assertEquals(0, new SubjectProgress("s", 0, 0, 0).getPercent());
        assertEquals(57, new OverallStats(2, 2, 2, 30, 17).getPercent());
    }

    @Test
    public void sessionDuration_handlesNormalRange() {
        StudySession session = new StudySession("id", "S1", 19 * 60, 20 * 60, 0, 0);
        assertEquals(60, session.getDurationMinutes());
    }

    @Test
    public void sessionDuration_handlesMidnightWrap() {
        // 11:00 PM -> 12:00 AM
        StudySession session = new StudySession("id", "S3", 23 * 60, 0, 0, 0);
        assertEquals(60, session.getDurationMinutes());
    }
}
