package hu.unideb.inf.kiadaskezelo.service.domain;

import hu.unideb.inf.kiadaskezelo.service.exception.HibasKeresException;
import org.springframework.data.domain.*;
import java.util.Set;

public final class Lapozas {
    private Lapozas() {}
    public static Pageable korlatoz(Pageable page, String... fields) {
        Set<String> allowed = Set.of(fields);
        for (Sort.Order order : page.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new HibasKeresException("Nem rendezhető mező: " + order.getProperty());
            }
        }
        Sort sort = page.getSort();
        if (sort.getOrderFor("id") == null) sort = sort.and(Sort.by("id"));
        return PageRequest.of(page.getPageNumber(), Math.min(page.getPageSize(), 100), sort);
    }
}
