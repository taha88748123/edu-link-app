package ma.naja7i.profile.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import ma.naja7i.shared.dto.Modality;
import ma.naja7i.shared.dto.ProfStatus;
import ma.naja7i.shared.dto.SchoolLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Custom search across profiles + prof_profiles + prof_subjects + prof_modalities + prof_locations.
 * Returns the list of matching prof user_ids; the service layer hydrates the views.
 */
@Repository
public class ProfCatalogRepository {

    @PersistenceContext
    private EntityManager em;

    public Page<UUID> searchProfIds(String subject,
                                    SchoolLevel level,
                                    Modality modality,
                                    String city,
                                    BigDecimal minStars,
                                    Pageable pageable) {

        StringBuilder where = new StringBuilder(
                " WHERE pp.status = :status ");
        Map<String, Object> params = new HashMap<>();
        params.put("status", ProfStatus.VALIDE.name());

        if (subject != null && !subject.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM prof_subjects ps " +
                    " WHERE ps.user_id = pp.user_id AND ps.subject_code = :subject ");
            params.put("subject", subject);
            if (level != null) {
                where.append(" AND ps.level = :level ");
                params.put("level", level.name());
            }
            where.append(") ");
        } else if (level != null) {
            where.append(" AND EXISTS (SELECT 1 FROM prof_subjects ps " +
                    " WHERE ps.user_id = pp.user_id AND ps.level = :level) ");
            params.put("level", level.name());
        }

        if (modality != null) {
            where.append(" AND EXISTS (SELECT 1 FROM prof_modalities pm " +
                    " WHERE pm.user_id = pp.user_id AND pm.modality = :modality) ");
            params.put("modality", modality.name());
        }

        if (city != null && !city.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM prof_locations pl " +
                    " WHERE pl.user_id = pp.user_id AND LOWER(pl.city) = LOWER(:city)) ");
            params.put("city", city);
        }

        if (minStars != null) {
            where.append(" AND COALESCE((SELECT rs.avg_stars FROM review_stats rs " +
                    " WHERE rs.prof_id = pp.user_id), 0) >= :minStars ");
            params.put("minStars", minStars);
        }

        // Order
        String orderBy = " ORDER BY pp.updated_at DESC ";
        if (pageable.getSort().isSorted()) {
            var order = pageable.getSort().iterator().next();
            String prop = order.getProperty();
            String dir = order.isAscending() ? "ASC" : "DESC";
            if ("stars".equalsIgnoreCase(prop)) {
                orderBy = " ORDER BY COALESCE((SELECT rs.avg_stars FROM review_stats rs " +
                        " WHERE rs.prof_id = pp.user_id), 0) " + dir + " ";
            } else if ("experience".equalsIgnoreCase(prop)) {
                orderBy = " ORDER BY pp.years_experience " + dir + " NULLS LAST ";
            } else if ("updated".equalsIgnoreCase(prop) || "updatedAt".equalsIgnoreCase(prop)) {
                orderBy = " ORDER BY pp.updated_at " + dir + " ";
            }
        }

        String sqlBase = "SELECT pp.user_id FROM prof_profiles pp " + where + orderBy;
        String countSql = "SELECT COUNT(*) FROM prof_profiles pp " + where;

        Query countQ = em.createNativeQuery(countSql);
        params.forEach(countQ::setParameter);
        Number total = (Number) countQ.getSingleResult();

        Query q = em.createNativeQuery(sqlBase);
        params.forEach(q::setParameter);
        q.setFirstResult((int) pageable.getOffset());
        q.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object> raw = q.getResultList();
        List<UUID> ids = new ArrayList<>(raw.size());
        for (Object o : raw) {
            if (o instanceof UUID u) ids.add(u);
            else if (o != null) ids.add(UUID.fromString(o.toString()));
        }

        return new PageImpl<>(ids, pageable, total.longValue());
    }
}
