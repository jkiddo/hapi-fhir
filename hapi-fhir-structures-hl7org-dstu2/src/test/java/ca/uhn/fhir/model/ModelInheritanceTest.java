package ca.uhn.fhir.model;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Block;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import org.hl7.fhir.dstu2.model.Address;
import org.hl7.fhir.dstu2.model.BackboneElement;
import org.hl7.fhir.dstu2.model.Base;
import org.hl7.fhir.dstu2.model.Binary;
import org.hl7.fhir.dstu2.model.BooleanType;
import org.hl7.fhir.dstu2.model.Bundle;
import org.hl7.fhir.dstu2.model.CodeType;
import org.hl7.fhir.dstu2.model.Coding;
import org.hl7.fhir.dstu2.model.DecimalType;
import org.hl7.fhir.dstu2.model.DomainResource;
import org.hl7.fhir.dstu2.model.Element;
import org.hl7.fhir.dstu2.model.Enumeration;
import org.hl7.fhir.dstu2.model.Extension;
import org.hl7.fhir.dstu2.model.IdType;
import org.hl7.fhir.dstu2.model.IntegerType;
import org.hl7.fhir.dstu2.model.List_;
import org.hl7.fhir.dstu2.model.Meta;
import org.hl7.fhir.dstu2.model.Money;
import org.hl7.fhir.dstu2.model.Narrative;
import org.hl7.fhir.dstu2.model.Parameters;
import org.hl7.fhir.dstu2.model.PrimitiveType;
import org.hl7.fhir.dstu2.model.Quantity;
import org.hl7.fhir.dstu2.model.Reference;
import org.hl7.fhir.dstu2.model.Resource;
import org.hl7.fhir.dstu2.model.StringType;
import org.hl7.fhir.dstu2.model.Timing;
import org.hl7.fhir.dstu2.model.Type;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBinary;
import org.hl7.fhir.instance.model.api.IBaseBooleanDatatype;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement;
import org.hl7.fhir.instance.model.api.IBaseDecimalDatatype;
import org.hl7.fhir.instance.model.api.IBaseEnumeration;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseHasModifierExtensions;
import org.hl7.fhir.instance.model.api.IBaseIntegerDatatype;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseXhtml;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.INarrative;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModelInheritanceTest {
  /*
   * <pre>
   * Other changes:
   *
   * Reference:
   *  * Add "resource" field, plus constructors and getter/setters for that field
   *
   * Narrative:
   *  * Add getValueAsDiv and setValueAsDiv
   *
   * XhtmlParser and XhtmlEncoder:
   *  * Do we need a better exception declaration?
   *
   * ElementDefinition
   *  * Backbone elements (eg .ElementDefinitionSlicingComponent) do not extend BackboneElement or have a @Block annotation for some reason
   *
   * Extension
   *  * Should URL not be StringType since it can't take extensions?
   * </pre>
   */

  private static FhirContext ourCtx = FhirContext.forDstu2Hl7Org();

  /**
   * This one should apply to all composite types
   */
  @Test
  public void testAddress() {
		assertTrue(ICompositeType.class.isAssignableFrom(Address.class));
  }

  @Test
  public void testBackboneElement() {
		assertTrue(IBaseBackboneElement.class.isAssignableFrom(BackboneElement.class));
		assertTrue(IBaseHasExtensions.class.isAssignableFrom(BackboneElement.class));
		assertTrue(IBaseHasModifierExtensions.class.isAssignableFrom(BackboneElement.class));
  }

  @Test
  public void testBase() {
		assertTrue(IBase.class.isAssignableFrom(Base.class));
  }

  @Test
  public void testBinary() {
		assertTrue(IBaseBinary.class.isAssignableFrom(Binary.class));
  }

  @Test
  public void testBooleanType() {
		assertTrue(IBaseBooleanDatatype.class.isAssignableFrom(BooleanType.class));
  }

  @Test
  public void testBundle() {
		assertTrue(IBaseBundle.class.isAssignableFrom(Bundle.class));
  }

  @Test
  public void testCoding() {
		assertTrue(IBaseCoding.class.isAssignableFrom(Coding.class));
  }

  @Test
  public void testDecimalType() {
		assertTrue(IBaseDecimalDatatype.class.isAssignableFrom(DecimalType.class));
  }

  @Test
  public void testDomainResource() {
		assertTrue(IBaseHasExtensions.class.isAssignableFrom(DomainResource.class));
		assertTrue(IBaseHasModifierExtensions.class.isAssignableFrom(DomainResource.class));
		assertTrue(IDomainResource.class.isAssignableFrom(DomainResource.class));
  }

  @Test
  public void testElement() {
		assertTrue(IBaseHasExtensions.class.isAssignableFrom(Element.class));
  }

  @Test
  public void testEnumeration() {
		assertTrue(IBaseEnumeration.class.isAssignableFrom(Enumeration.class));

    DatatypeDef def = Enumeration.class.getAnnotation(DatatypeDef.class);
		assertTrue(def.isSpecialization());
  }

  /**
   * Should be "implements IBaseExtension<Extension>"
   */
  @Test
  public void testExtension() {
		assertTrue(IBaseExtension.class.isAssignableFrom(Extension.class));
		assertTrue(IBaseHasExtensions.class.isAssignableFrom(Extension.class));
  }

  @Test
  public void testIdType() {
		assertTrue(IIdType.class.isAssignableFrom(IdType.class));
  }

  @Test
  public void testIntegerType() {
		assertTrue(IBaseIntegerDatatype.class.isAssignableFrom(IntegerType.class));
  }

  @Test
  public void testList() {
		assertEquals("List", ourCtx.getResourceDefinition(List_.class).getName());
  }

  @Test
  public void testMeta() {
		assertTrue(IBaseMetaType.class.isAssignableFrom(Meta.class));
  }

  @Test
  public void testNarrative() {
		assertTrue(INarrative.class.isAssignableFrom(Narrative.class));
  }

  @Test
  public void testParameters() {
		assertTrue(IBaseParameters.class.isAssignableFrom(Parameters.class));
  }

  @Test
  public void testPrimitiveType() {
		assertTrue(IPrimitiveType.class.isAssignableFrom(PrimitiveType.class));
		assertTrue(IBaseHasExtensions.class.isAssignableFrom(PrimitiveType.class));
  }

  @Test
  public void testProfiledDatatype() {
		assertEquals(StringType.class, CodeType.class.getSuperclass());
		assertEquals(StringType.class, CodeType.class.getAnnotation(DatatypeDef.class).profileOf());
		assertEquals(Quantity.class, Money.class.getSuperclass());
		assertEquals(Quantity.class, Money.class.getAnnotation(DatatypeDef.class).profileOf());
  }

  @Test
  public void testReference() {
		assertTrue(IBaseReference.class.isAssignableFrom(Reference.class));
  }

  @Test
  public void testResource() {
		assertTrue(IAnyResource.class.isAssignableFrom(Resource.class));
  }

  @Test
  public void testTiming_TimingRepeatComponent() {
		assertTrue(IBaseDatatypeElement.class.isAssignableFrom(Timing.TimingRepeatComponent.class));
		assertNotNull(Timing.TimingRepeatComponent.class.getAnnotation(Block.class));
  }

  @Test
  public void testType() {
		assertTrue(IBaseDatatype.class.isAssignableFrom(Type.class));
  }

  @Test
  public void testXhtml() {
		assertTrue(IBaseXhtml.class.isAssignableFrom(XhtmlNode.class));
  }

}
