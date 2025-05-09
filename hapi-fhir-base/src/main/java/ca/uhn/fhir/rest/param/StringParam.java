/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ca.uhn.fhir.rest.param;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.primitive.StringDt;
import ca.uhn.fhir.rest.api.Constants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.defaultString;

public class StringParam extends BaseParam implements IQueryParameterType {

	private static final Logger ourLog = LoggerFactory.getLogger(StringParam.class);

	private boolean myText;
	private boolean myContains;
	private boolean myExact;
	private String myValue;

	private Boolean myNicknameExpand;

	/**
	 * Constructor
	 */
	public StringParam() {}

	/**
	 * Constructor
	 */
	public StringParam(String theValue) {
		setValue(theValue);
	}

	/**
	 * Constructor
	 */
	public StringParam(String theValue, boolean theExact) {
		setValue(theValue);
		setExact(theExact);
	}

	@Override
	String doGetQueryParameterQualifier() {
		if (isExact()) {
			return Constants.PARAMQUALIFIER_STRING_EXACT;
		} else if (isContains()) {
			return Constants.PARAMQUALIFIER_STRING_CONTAINS;
		} else if (isText()) {
			return Constants.PARAMQUALIFIER_STRING_TEXT;
		} else {
			return null;
		}
	}

	@Override
	String doGetValueAsQueryToken(FhirContext theContext) {
		return ParameterUtil.escape(myValue);
	}

	public boolean isNicknameExpand() {
		return myNicknameExpand != null && myNicknameExpand;
	}

	public StringParam setNicknameExpand(boolean theNicknameExpand) {
		myNicknameExpand = theNicknameExpand;
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37)
				.append(myExact)
				.append(myText)
				.append(myContains)
				.append(myValue)
				.append(getMissing())
				.toHashCode();
	}

	@Override
	void doSetValueAsQueryToken(FhirContext theContext, String theParamName, String theQualifier, String theValue) {
		if (Constants.PARAMQUALIFIER_NICKNAME.equals(theQualifier)) {
			myNicknameExpand = true;
			theQualifier = "";

			if (!("name".equals(theParamName) || "given".equals(theParamName))) {
				ourLog.debug(
						":nickname qualifier was assigned to a search parameter other than one of the intended parameters \"name\" and \"given\"");
			}
		}

		if (Constants.PARAMQUALIFIER_STRING_EXACT.equals(theQualifier)) {
			setExact(true);
		} else {
			setExact(false);
		}
		if (Constants.PARAMQUALIFIER_STRING_CONTAINS.equals(theQualifier)) {
			setContains(true);
		} else {
			setContains(false);
		}

		setText(Constants.PARAMQUALIFIER_STRING_TEXT.equals(theQualifier));

		myValue = ParameterUtil.unescape(theValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof StringParam)) {
			return false;
		}

		StringParam other = (StringParam) obj;

		EqualsBuilder eb = new EqualsBuilder();
		eb.append(myExact, other.myExact);
		eb.append(myText, other.myText);
		eb.append(myContains, other.myContains);
		eb.append(myValue, other.myValue);
		eb.append(getMissing(), other.getMissing());

		return eb.isEquals();
	}

	public String getValue() {
		return myValue;
	}

	public StringParam setValue(String theValue) {
		myValue = theValue;
		return this;
	}

	public StringDt getValueAsStringDt() {
		return new StringDt(myValue);
	}

	public String getValueNotNull() {
		return defaultString(myValue);
	}

	public boolean isText() {
		return myText;
	}

	public void setText(boolean theText) {
		myText = theText;
		if (myText) {
			setContains(false);
			setExact(false);
			setMissing(null);
		}
	}

	/**
	 * String parameter modifier <code>:contains</code>
	 */
	public boolean isContains() {
		return myContains;
	}

	/**
	 * String parameter modifier <code>:contains</code>
	 */
	public StringParam setContains(boolean theContains) {
		myContains = theContains;
		if (myContains) {
			setText(false);
			setExact(false);
			setMissing(null);
		}
		return this;
	}

	public boolean isEmpty() {
		return StringUtils.isEmpty(myValue);
	}

	public boolean isExact() {
		return myExact;
	}

	public StringParam setExact(boolean theExact) {
		myExact = theExact;
		if (myExact) {
			setText(false);
			setContains(false);
			setMissing(null);
		}
		return this;
	}

	@Override
	public String toString() {
		ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		builder.append("value", getValue());
		if (myText) {
			builder.append("text", myText);
		}
		if (myExact) {
			builder.append("exact", myExact);
		}
		if (myContains) {
			builder.append("contains", myContains);
		}
		if (getMissing() != null) {
			builder.append("missing", getMissing().booleanValue());
		}
		return builder.toString();
	}
}
