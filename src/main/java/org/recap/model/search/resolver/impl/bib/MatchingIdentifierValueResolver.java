package org.recap.model.search.resolver.impl.bib;

import org.recap.model.search.resolver.BibValueResolver;
import org.recap.model.solr.BibItem;

public class MatchingIdentifierValueResolver implements BibValueResolver {
    @Override
    public Boolean isInterested(String field) {
        return "MatchingIdentifier".equalsIgnoreCase(field);
    }

    @Override
    public void setValue(BibItem bibItem, Object value) {
        bibItem.setMatchingIdentifier((String) value);
    }
}
