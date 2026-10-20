package model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PythonScript {
    PDF_TO_WORD("pdf-to-word.py"),
    WORD_TO_PDF("word-to-pdf.py");

    final String nameScript;
}
