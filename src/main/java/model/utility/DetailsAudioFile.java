package model.utility;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DetailsAudioFile {
    private String  fileName, path, tag, title, artist, albumArtist, album, track,
            discNumber, year, genre, comment, codec, bitrate, frequency;

    private LocalDate modified;
    private LocalTime length;
}
