// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.xml;

import static javax.xml.stream.XMLStreamConstants.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.xml.annotation.XmlFormat.*;

import java.lang.reflect.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Parses text generated by the {@link XmlSerializer} class back into a POJO model.
 *
 * <h5 class='section'>Media types:</h5>
 * <p>
 * Handles <code>Content-Type</code> types: <code>text/xml</code>
 *
 * <h5 class='section'>Description:</h5>
 * <p>
 * See the {@link XmlSerializer} class for a description of Juneau-generated XML.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlParserContext}
 * 	<li>{@link BeanContext}
 * </ul>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Consumes("text/xml,application/xml")
public class XmlParser extends ReaderParser {

	/** Default parser, all default settings.*/
	public static final XmlParser DEFAULT = new XmlParser(PropertyStore.create());

	private static final int UNKNOWN=0, OBJECT=1, ARRAY=2, STRING=3, NUMBER=4, BOOLEAN=5, NULL=6;


	private final XmlParserContext ctx;

	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public XmlParser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(XmlParserContext.class);
	}

	@Override /* CoreObject */
	public XmlParserBuilder builder() {
		return new XmlParserBuilder(propertyStore);
	}

	/**
	 * Workhorse method.
	 *
	 * @param session The current parser session.
	 * @param eType The expected type of object.
	 * @param currAttr The current bean property name.
	 * @param r The reader.
	 * @param outer The outer object.
	 * @param isRoot If <jk>true</jk>, then we're serializing a root element in the document.
	 * @param pMeta The bean property metadata.
	 * @return The parsed object.
	 * @throws Exception
	 */
	protected <T> T parseAnything(XmlParserSession session, ClassMeta<T> eType, String currAttr, XMLStreamReader r, Object outer, boolean isRoot, BeanPropertyMeta pMeta) throws Exception {

		if (eType == null)
			eType = (ClassMeta<T>)object();
		PojoSwap<T,Object> transform = (PojoSwap<T,Object>)eType.getPojoSwap();
		ClassMeta<?> sType = eType.getSerializedClassMeta();
		session.setCurrentClass(sType);

		String wrapperAttr = (isRoot && session.isPreserveRootElement()) ? r.getName().getLocalPart() : null;
		String typeAttr = r.getAttributeValue(null, session.getBeanTypePropertyName());
		int jsonType = getJsonType(typeAttr);
		String elementName = session.getElementName(r);
		if (jsonType == 0) {
			if (elementName == null || elementName.equals(currAttr))
				jsonType = UNKNOWN;
			else {
				typeAttr = elementName;
				jsonType = getJsonType(elementName);
			}
		}

		ClassMeta tcm = session.getClassMeta(typeAttr, pMeta, eType);
		if (tcm == null && elementName != null && ! elementName.equals(currAttr))
			tcm = session.getClassMeta(elementName, pMeta, eType);
		if (tcm != null)
			sType = eType = tcm;

		Object o = null;

		if (jsonType == NULL) {
			r.nextTag();	// Discard end tag
			return null;
		}

		if (sType.isObject()) {
			if (jsonType == OBJECT) {
				ObjectMap m = new ObjectMap(session);
				parseIntoMap(session, r, m, string(), object(), pMeta);
				if (wrapperAttr != null)
					m = new ObjectMap(session).append(wrapperAttr, m);
				o = session.cast(m, pMeta, eType);
			} else if (jsonType == ARRAY)
				o = parseIntoCollection(session, r, new ObjectList(session), object(), pMeta);
			else if (jsonType == STRING) {
				o = session.getElementText(r);
				if (sType.isChar())
					o = o.toString().charAt(0);
			}
			else if (jsonType == NUMBER)
				o = parseNumber(session.getElementText(r), null);
			else if (jsonType == BOOLEAN)
				o = Boolean.parseBoolean(session.getElementText(r));
			else if (jsonType == UNKNOWN)
				o = getUnknown(session, r);
		} else if (sType.isBoolean()) {
			o = Boolean.parseBoolean(session.getElementText(r));
		} else if (sType.isCharSequence()) {
			o = session.getElementText(r);
		} else if (sType.isChar()) {
			String s = session.getElementText(r);
			o = s.length() == 0 ? 0 : s.charAt(0);
		} else if (sType.isMap()) {
			Map m = (sType.canCreateNewInstance(outer) ? (Map)sType.newInstance(outer) : new ObjectMap(session));
			o = parseIntoMap(session, r, m, sType.getKeyType(), sType.getValueType(), pMeta);
			if (wrapperAttr != null)
				o = new ObjectMap(session).append(wrapperAttr, m);
		} else if (sType.isCollection()) {
			Collection l = (sType.canCreateNewInstance(outer) ? (Collection)sType.newInstance(outer) : new ObjectList(session));
			o = parseIntoCollection(session, r, l, sType.getElementType(), pMeta);
		} else if (sType.isNumber()) {
			o = parseNumber(session.getElementText(r), (Class<? extends Number>)sType.getInnerClass());
		} else if (sType.canCreateNewBean(outer)) {
			if (sType.getExtendedMeta(XmlClassMeta.class).getFormat() == COLLAPSED) {
				String fieldName = r.getLocalName();
				BeanMap<?> m = session.newBeanMap(outer, sType.getInnerClass());
				BeanPropertyMeta bpm = m.getMeta().getExtendedMeta(XmlBeanMeta.class).getPropertyMeta(fieldName);
				ClassMeta<?> cm = m.getMeta().getClassMeta();
				Object value = parseAnything(session, cm, currAttr, r, m.getBean(false), false, null);
				setName(cm, value, currAttr);
				bpm.set(m, value);
				o = m.getBean();
			} else {
				BeanMap m = session.newBeanMap(outer, sType.getInnerClass());
				o = parseIntoBean(session, r, m).getBean();
			}
		} else if (sType.isArray()) {
			ArrayList l = (ArrayList)parseIntoCollection(session, r, new ArrayList(), sType.getElementType(), pMeta);
			o = session.toArray(sType, l);
		} else if (sType.canCreateNewInstanceFromString(outer)) {
			o = sType.newInstanceFromString(outer, session.getElementText(r));
		} else if (sType.canCreateNewInstanceFromNumber(outer)) {
			o = sType.newInstanceFromNumber(session, outer, parseNumber(session.getElementText(r), sType.getNewInstanceFromNumberClass()));
		} else {
			throw new ParseException(session, "Class ''{0}'' could not be instantiated.  Reason: ''{1}'', property: ''{2}''", sType.getInnerClass().getName(), sType.getNotABeanReason(), pMeta == null ? null : pMeta.getName());
		}

		if (transform != null && o != null)
			o = transform.unswap(session, o, eType);

		if (outer != null)
			setParent(eType, o, outer);

		return (T)o;
	}

	private <K,V> Map<K,V> parseIntoMap(XmlParserSession session, XMLStreamReader r, Map<K,V> m, ClassMeta<K> keyType, ClassMeta<V> valueType, BeanPropertyMeta pMeta) throws Exception {
		int depth = 0;
		for (int i = 0; i < r.getAttributeCount(); i++) {
			String a = r.getAttributeLocalName(i);
			// TODO - Need better handling of namespaces here.
			if (! (a.equals(session.getBeanTypePropertyName()))) {
				K key = session.trim(convertAttrToType(session, m, a, keyType));
				V value = session.trim(convertAttrToType(session, m, r.getAttributeValue(i), valueType));
				setName(valueType, value, key);
				m.put(key, value);
			}
		}
		do {
			int event = r.nextTag();
			String currAttr;
			if (event == START_ELEMENT) {
				depth++;
				currAttr = session.getElementName(r);
				K key = convertAttrToType(session, m, currAttr, keyType);
				V value = parseAnything(session, valueType, currAttr, r, m, false, pMeta);
				setName(valueType, value, currAttr);
				if (valueType.isObject() && m.containsKey(key)) {
					Object o = m.get(key);
					if (o instanceof List)
						((List)o).add(value);
					else
						m.put(key, (V)new ObjectList(o, value).setBeanSession(session));
				} else {
					m.put(key, value);
				}
			} else if (event == END_ELEMENT) {
				depth--;
				return m;
			}
		} while (depth > 0);
		return m;
	}

	private <E> Collection<E> parseIntoCollection(XmlParserSession session, XMLStreamReader r, Collection<E> l, ClassMeta<E> elementType, BeanPropertyMeta pMeta) throws Exception {
		int depth = 0;
		do {
			int event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				E value = parseAnything(session, elementType, null, r, l, false, pMeta);
				l.add(value);
			} else if (event == END_ELEMENT) {
				depth--;
				return l;
			}
		} while (depth > 0);
		return l;
	}

	private Object[] doParseArgs(XmlParserSession session, XMLStreamReader r, ClassMeta<Object[]> args) throws Exception {
		int depth = 0;
		ClassMeta<?>[] argTypes = args.getArgs();
		Object[] o = new Object[argTypes.length];
		int i = 0;
		do {
			int event = r.nextTag();
			if (event == START_ELEMENT) {
				depth++;
				o[i] = parseAnything(session, argTypes[i], null, r, null, false, null);
				i++;
			} else if (event == END_ELEMENT) {
				depth--;
				return o;
			}
		} while (depth > 0);
		return o;
	}

	private static int getJsonType(String s) {
		if (s == null)
			return UNKNOWN;
		char c = s.charAt(0);
		switch(c) {
			case 'o': return (s.equals("object") ? OBJECT : UNKNOWN);
			case 'a': return (s.equals("array") ? ARRAY : UNKNOWN);
			case 's': return (s.equals("string") ? STRING : UNKNOWN);
			case 'b': return (s.equals("boolean") ? BOOLEAN : UNKNOWN);
			case 'n': {
				c = s.charAt(2);
				switch(c) {
					case 'm': return (s.equals("number") ? NUMBER : UNKNOWN);
					case 'l': return (s.equals("null") ? NULL : UNKNOWN);
				}
				//return NUMBER;
			}
		}
		return UNKNOWN;
	}

	private <T> BeanMap<T> parseIntoBean(XmlParserSession session, XMLStreamReader r, BeanMap<T> m) throws Exception {
		BeanMeta<?> bMeta = m.getMeta();
		XmlBeanMeta xmlMeta = bMeta.getExtendedMeta(XmlBeanMeta.class);

		for (int i = 0; i < r.getAttributeCount(); i++) {
			String key = session.getAttributeName(r, i);
			String val = r.getAttributeValue(i);
			BeanPropertyMeta bpm = xmlMeta.getPropertyMeta(key);
			if (bpm == null) {
				if (xmlMeta.getAttrsProperty() != null) {
					xmlMeta.getAttrsProperty().add(m, key, val);
				} else {
					Location l = r.getLocation();
					onUnknownProperty(session, key, m, l.getLineNumber(), l.getColumnNumber());
				}
			} else {
				bpm.set(m, val);
			}
		}

		BeanPropertyMeta cp = xmlMeta.getContentProperty();
		XmlFormat cpf = xmlMeta.getContentFormat();
		boolean trim = cp == null || ! cpf.isOneOf(MIXED_PWS, TEXT_PWS);
		ClassMeta<?> cpcm = (cp == null ? session.object() : cp.getClassMeta());
		StringBuilder sb = null;
		BeanRegistry breg = cp == null ? null : cp.getBeanRegistry();
		LinkedList<Object> l = null;

		int depth = 0;
		do {
			int event = r.next();
			String currAttr;
			// We only care about text in MIXED mode.
			// Ignore if in ELEMENTS mode.
			if (event == CHARACTERS) {
				if (cp != null && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (cpcm.isCollectionOrArray()) {
						if (l == null)
							l = new LinkedList<Object>();
						l.add(session.getText(r, false));
					} else {
						cp.set(m, session.getText(r, trim));
					}
				} else if (cpf != ELEMENTS) {
					String s = session.getText(r, trim);
					if (s != null) {
						if (sb == null)
							sb = session.getStringBuilder();
						sb.append(s);
					}
				} else {
					// Do nothing...we're in ELEMENTS mode.
				}
			} else if (event == START_ELEMENT) {
				if (cp != null && cpf.isOneOf(TEXT, TEXT_PWS)) {
					String s = session.parseText(r);
					if (s != null) {
						if (sb == null)
							sb = session.getStringBuilder();
						sb.append(s);
					}
					depth--;
				} else if (cpf == XMLTEXT) {
					if (sb == null)
						sb = session.getStringBuilder();
					sb.append(session.getElementAsString(r));
					depth++;
				} else if (cp != null && cpf.isOneOf(MIXED, MIXED_PWS)) {
					if (session.isWhitespaceElement(r) && (breg == null || ! breg.hasName(r.getLocalName()))) {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<Object>();
							l.add(session.parseWhitespaceElement(r));
						} else {
							cp.set(m, session.parseWhitespaceElement(r));
						}
					} else {
						if (cpcm.isCollectionOrArray()) {
							if (l == null)
								l = new LinkedList<Object>();
							l.add(parseAnything(session, cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
						} else {
							cp.set(m, parseAnything(session, cpcm, cp.getName(), r, m.getBean(false), false, cp));
						}
					}
				} else if (cp != null && cpf == ELEMENTS) {
					cp.add(m, parseAnything(session, cpcm.getElementType(), cp.getName(), r, m.getBean(false), false, cp));
				} else {
					currAttr = session.getElementName(r);
					BeanPropertyMeta pMeta = xmlMeta.getPropertyMeta(currAttr);
					if (pMeta == null) {
						Location loc = r.getLocation();
						onUnknownProperty(session, currAttr, m, loc.getLineNumber(), loc.getColumnNumber());
						skipCurrentTag(r);
					} else {
						session.setCurrentProperty(pMeta);
						XmlFormat xf = pMeta.getExtendedMeta(XmlBeanPropertyMeta.class).getXmlFormat();
						if (xf == COLLAPSED) {
							ClassMeta<?> et = pMeta.getClassMeta().getElementType();
							Object value = parseAnything(session, et, currAttr, r, m.getBean(false), false, pMeta);
							setName(et, value, currAttr);
							pMeta.add(m, value);
						} else if (xf == ATTR)  {
							pMeta.set(m, session.getAttributeValue(r, 0));
							r.nextTag();
						} else {
							ClassMeta<?> cm = pMeta.getClassMeta();
							Object value = parseAnything(session, cm, currAttr, r, m.getBean(false), false, pMeta);
							setName(cm, value, currAttr);
							pMeta.set(m, value);
						}
						session.setCurrentProperty(null);
					}
				}
			} else if (event == END_ELEMENT) {
				if (depth > 0) {
					if (cpf == XMLTEXT) {
						if (sb == null)
							sb = session.getStringBuilder();
						sb.append(session.getElementAsString(r));
					}
					else
						throw new ParseException("End element found where one was not expected.  {0}", XmlUtils.toReadableEvent(r));
				}
				depth--;
			} else {
				throw new ParseException("Unexpected event type: {0}", XmlUtils.toReadableEvent(r));
			}
		} while (depth >= 0);

		if (sb != null && cp != null)
			cp.set(m, sb.toString());
		else if (l != null && cp != null)
			cp.set(m, XmlUtils.collapseTextNodes(l));

		session.returnStringBuilder(sb);
		return m;
	}

	private static void skipCurrentTag(XMLStreamReader r) throws XMLStreamException {
		int depth = 1;
		do {
			int event = r.next();
			if (event == START_ELEMENT)
				depth++;
			else if (event == END_ELEMENT)
				depth--;
		} while (depth > 0);
	}

	private Object getUnknown(XmlParserSession session, XMLStreamReader r) throws Exception {
		if (r.getEventType() != XMLStreamConstants.START_ELEMENT) {
			throw new XMLStreamException("parser must be on START_ELEMENT to read next text", r.getLocation());
		}
		ObjectMap m = null;

		// If this element has attributes, then it's always an ObjectMap.
		if (r.getAttributeCount() > 0) {
			m = new ObjectMap(session);
			for (int i = 0; i < r.getAttributeCount(); i++) {
				String key = session.getAttributeName(r, i);
				String val = r.getAttributeValue(i);
				if (! key.equals(session.getBeanTypePropertyName()))
					m.put(key, val);
			}
		}
		int eventType = r.next();
		StringBuilder sb = session.getStringBuilder();
		while (eventType != XMLStreamConstants.END_ELEMENT) {
			if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA || eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
				sb.append(r.getText());
			} else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType == XMLStreamConstants.COMMENT) {
				// skipping
			} else if (eventType == XMLStreamConstants.END_DOCUMENT) {
				throw new XMLStreamException("Unexpected end of document when reading element text content", r.getLocation());
			} else if (eventType == XMLStreamConstants.START_ELEMENT) {
				// Oops...this has an element in it.
				// Parse it as a map.
				if (m == null)
					m = new ObjectMap(session);
				int depth = 0;
				do {
					int event = (eventType == -1 ? r.nextTag() : eventType);
					String currAttr;
					if (event == START_ELEMENT) {
						depth++;
						currAttr = session.getElementName(r);
						String key = convertAttrToType(session, null, currAttr, string());
						Object value = parseAnything(session, object(), currAttr, r, null, false, null);
						if (m.containsKey(key)) {
							Object o = m.get(key);
							if (o instanceof ObjectList)
								((ObjectList)o).add(value);
							else
								m.put(key, new ObjectList(o, value).setBeanSession(session));
						} else {
							m.put(key, value);
						}

					} else if (event == END_ELEMENT) {
						depth--;
						break;
					}
					eventType = -1;
				} while (depth > 0);
				break;
			} else {
				throw new XMLStreamException("Unexpected event type " + eventType, r.getLocation());
			}
			eventType = r.next();
		}
		String s = sb.toString();
		session.returnStringBuilder(sb);
		s = session.decodeString(s);
		if (m != null) {
			if (! s.isEmpty())
				m.put("contents", s);
			return m;
		}
		return s;
	}


	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Parser */
	public XmlParserSession createSession(Object input, ObjectMap op, Method javaMethod, Object outer, Locale locale, TimeZone timeZone, MediaType mediaType) {
		return new XmlParserSession(ctx, op, input, javaMethod, outer, locale, timeZone, mediaType);
	}

	@Override /* Parser */
	protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		return parseAnything(s, type, null, s.getXmlStreamReader(), s.getOuter(), true, null);
	}

	@Override /* ReaderParser */
	protected <K,V> Map<K,V> doParseIntoMap(ParserSession session, Map<K,V> m, Type keyType, Type valueType) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		ClassMeta cm = session.getClassMeta(m.getClass(), keyType, valueType);
		return parseIntoMap(s, m, cm.getKeyType(), cm.getValueType());
	}

	@Override /* ReaderParser */
	protected <E> Collection<E> doParseIntoCollection(ParserSession session, Collection<E> c, Type elementType) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		ClassMeta cm = session.getClassMeta(c.getClass(), elementType);
		return parseIntoCollection(s,c, cm.getElementType());
	}

	@Override /* ReaderParser */
	protected Object[] doParseArgs(ParserSession session, ClassMeta<Object[]> args) throws Exception {
		XmlParserSession s = (XmlParserSession)session;
		return doParseArgs(s, s.getXmlStreamReader(), args);
	}
}
