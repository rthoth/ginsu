package com.github.rthoth.ginsu;

import com.google.common.truth.Fact;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequences;

import static com.google.common.truth.Fact.simpleFact;

public final class CoordinateSequenceSubject extends Subject {

	private final CoordinateSequence actual;

	public static Factory<CoordinateSequenceSubject, CoordinateSequence> sequences() {
		return CoordinateSequenceSubject::new;
	}

	private CoordinateSequenceSubject(FailureMetadata metadata, CoordinateSequence actual) {
		super(metadata, actual);
		this.actual = actual;
	}

	public void isEqual(CoordinateSequence sequence) {
		if (!CoordinateSequences.isEqual(actual, sequence)) {
			failWithActual(simpleFact("expected: " + sequence.toString()));
		}
	}

	public static CoordinateSequenceSubject assertThat(CoordinateSequence sequence) {
		return Truth.assertWithMessage("Invalid CoordinateSequence")
			.about(sequences()).that(sequence);
	}
}
