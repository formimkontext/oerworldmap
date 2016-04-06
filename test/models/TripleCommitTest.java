package models;

import static org.junit.Assert.*;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;

/**
 * Created by fo on 11.02.16.
 */
public class TripleCommitTest {

  @Test
  public void testPlainLiteralLine() {
    TripleCommit.Diff.fromString(
        " + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

  @Test
  public void testAddLanguageLiteralLine() {
    TripleCommit.Diff
        .fromString("+ <http://example.org/show/218> <http://example.org/show/localName> \"That Seventies Show\"@en .");
  }

  @Test
  public void testAddTypedLiteralLine() {
    TripleCommit.Diff.fromString(
      "+ <http://en.wikipedia.org/wiki/Helium> <http://example.org/elements/specificGravity> \"1.663E-4\"^^<http://www.w3.org/2001/XMLSchema#double> .");
  }

  @Test(expected = RiotException.class)
  public void testInvalidLiteral() {
    TripleCommit.Diff.fromString(
        " + <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show .");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidOp() {
    TripleCommit.Diff.fromString(
        " | <http://example.org/show/218> <http://www.w3.org/2000/01/rdf-schema#label> \"That Seventies Show\" .");
  }

  @Test
  public void testReadHeader() {

    TripleCommit.Header header = TripleCommit.Header.fromString( //
      "Author: unittest@oerworldmap.org\n" //
        + "Date: " + ZonedDateTime.now().toString() + "\n");
    Assert.assertNotNull(header);

  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingAuthorHeader() {
    TripleCommit.Header.fromString("Date: " + ZonedDateTime.now().toString() + "\n");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMissingDateHeader() {
    TripleCommit.Header.fromString("Author: Foo Bar <foo@bar.de>");
  }

  @Test
  public void testValidCommit() {
    TripleCommit commit = TripleCommit.fromString(
      "Author: Foo Bar <foo@bar.de>\n" +
        "Date: 2007-12-03T10:15:30+01:00\n" +
        "\n" +
        "+ <urn:uuid:foo> <urn:uuid:bar> <urn:uuid:baz> .");
    assertNotNull(commit);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidCommit() {
    TripleCommit commit = TripleCommit.fromString(
      "Author: Foo Bar <foo@bar.de>\n" +
        "Date: 2007-12-03T10:15:30+01:00\n" +
        "+ <urn:uuid:foo> <urn:uuid:bar> <urn:uuid:baz> .");
    assertNotNull(commit);
  }

  @Test
  public void testApplyDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    Model actual = ModelFactory.createDefaultModel();
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple));
    diff.apply(actual);
    Model expected = ModelFactory.createDefaultModel();
    expected.read(new ByteArrayInputStream(ntriple.getBytes()), null, Lang.NTRIPLES.getName());
    assertTrue(expected.isIsomorphicWith(actual));
  }

  @Test
  public void testUnapplyDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    Model actual = ModelFactory.createDefaultModel();
    actual.read(new ByteArrayInputStream(ntriple.getBytes()), null, Lang.NTRIPLES.getName());
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple));
    diff.unapply(actual);
    Model expected = ModelFactory.createDefaultModel();
    assertTrue(expected.isIsomorphicWith(actual));
  }

  @Test
  public void testReverseDiff() {
    String ntriple = "<info:subject> <info:predicate> <info:object> .";
    TripleCommit.Diff diff = TripleCommit.Diff.fromString("+ ".concat(ntriple)).reverse();
    assertEquals(1, diff.getLines().size());
    assertFalse(diff.getLines().get(0).add);
  }

}