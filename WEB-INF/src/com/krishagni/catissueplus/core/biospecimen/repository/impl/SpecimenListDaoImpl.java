package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListDao;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class SpecimenListDaoImpl extends AbstractDao<SpecimenList> implements SpecimenListDao {

	@Override
	public Class<SpecimenList> getType() {
		return SpecimenList.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SpecimenListSummary> getSpecimenLists(SpecimenListsCriteria crit) {
		List<SpecimenListSummary> results = new ArrayList<>();
		Map<Long, SpecimenListSummary> listMap = new HashMap<>();

		Query query = getSpecimenListsQuery(crit, false)
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults());

		List<SpecimenList> lists = query.list();
		for (SpecimenList list : lists) {
			SpecimenListSummary summary = SpecimenListSummary.fromSpecimenList(list);
			results.add(summary);

			if (crit.includeStat()) {
				listMap.put(list.getId(), summary);
			}
		}

		if (listMap.isEmpty()) {
			return results;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_LIST_SPECIMENS_COUNT)
			.setParameterList("listIds", listMap.keySet())
			.list();

		for (Object[] row : rows) {
			SpecimenListSummary summary = listMap.get((Long)row[0]);
			summary.setSpecimenCount(((Number)row[1]).intValue());
		}

		return results;
	}

	@Override
	public Long getSpecimenListsCount(SpecimenListsCriteria crit) {
		return ((Number) getSpecimenListsQuery(crit, true).uniqueResult()).longValue();
	}

	@Override
	public SpecimenList getSpecimenList(Long listId) {
		return (SpecimenList)sessionFactory.getCurrentSession()
				.get(SpecimenList.class, listId);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public SpecimenList getSpecimenListByName(String name) {
		List<SpecimenList> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_SPECIMEN_LIST_BY_NAME)
				.setString("name", name)
				.list();
		
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public SpecimenList getDefaultSpecimenList(Long userId) {
		return getSpecimenListByName(SpecimenList.getDefaultListName(userId));
	}

	@SuppressWarnings("unchecked")
	@Override
	public int getListSpecimensCount(Long listId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_LIST_SPECIMENS_COUNT)
			.setParameterList("listIds", Collections.singletonList(listId))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return 0;
		}

		return ((Number)rows.iterator().next()[1]).intValue();
	}

	@Override
	public void deleteSpecimenList(SpecimenList list) {
		sessionFactory.getCurrentSession().delete(list);
	}

	//
	// specimen list items
	//
	@Override
	public void saveListItems(List<SpecimenListItem> items) {
		int i = 0;
		for (SpecimenListItem item : items) {
			getCurrentSession().saveOrUpdate(item);
			i++;

			if (i % 50 == 0) {
				getCurrentSession().flush();
				getCurrentSession().clear();
			}
		}
	}

	@Override
	public int deleteListItems(Long listId, List<Long> specimenIds) {
		return getCurrentSession().getNamedQuery(DELETE_LIST_SPECIMENS)
			.setParameter("listId", listId)
			.setParameterList("specimenIds", specimenIds)
			.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getSpecimenIdsInList(Long listId, List<Long> specimenIds) {
		Criteria query = getCurrentSession().createCriteria(SpecimenList.class, "list")
			.createAlias("list.specimens", "item")
			.createAlias("item.specimen", "specimen")
			.setProjection(Projections.property("specimen.id"))
			.add(Restrictions.eq("list.id", listId));
		applyIdsFilter(query, "specimen.id", specimenIds);
		return query.list();
	}

	@Override
	public void addChildSpecimens(Long listId, boolean oracle) {
		getCurrentSession().getNamedQuery(oracle ? ADD_CHILD_SPECIMENS_ORA : ADD_CHILD_SPECIMENS_MYSQL)
			.setParameter("listId", listId)
			.executeUpdate();
	}

	@Override
	@Deprecated
	public Map<Long, List<Specimen>> getListCpSpecimens(Long listId) {
		List<Object[]> rows = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_LIST_CP_SPECIMENS)
			.setLong("listId", listId)
			.list();

		Map<Long, List<Specimen>> cpSpecimens = new HashMap<Long, List<Specimen>>();
		for (Object[] row : rows) {
			Long cpId = (Long)row[0];
			Specimen specimen = (Specimen)row[1];

			List<Specimen> specimens = cpSpecimens.get(cpId);
			if (specimens == null) {
				specimens = new ArrayList<>();
				cpSpecimens.put(cpId, specimens);
			}

			specimens.add(specimen);
		}

		return cpSpecimens;
	}

	@Override
	@Deprecated
	public List<Long> getListSpecimensCpIds(Long listId) {
		return sessionFactory.getCurrentSession().getNamedQuery(GET_LIST_SPMNS_CP_IDS)
			.setLong("listId", listId)
			.list();
	}

	private Query getSpecimenListsQuery(SpecimenListsCriteria crit, boolean countReq) {
		Query query = getCurrentSession().createQuery(getHql(crit, countReq));
		return setParams(query, crit);
	}

	private String getHql(SpecimenListsCriteria crit, boolean countReq) {
		StringBuilder hql = new StringBuilder(countReq ? "select count(distinct l.id) from " : "select distinct l from ")
			.append(getType().getName()).append(" l");

		if (crit.userId() != null) {
			hql.append(" join l.owner owner").append(" left join l.sharedWith sharedWith");
		}

		hql.append(" where l.deletedOn is null");

		if (crit.userId() != null) {
			hql.append(" and (owner.id = :userId or sharedWith.id = :userId)");
		}

		if (StringUtils.isNotBlank(crit.query())) {
			hql.append(" and upper(l.name) like :name");
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			hql.append(" and l.id in (:ids)");
		}

		hql.append(" order by l.lastUpdatedOn desc");
		return hql.toString();
	}

	private Query setParams(Query query, SpecimenListsCriteria crit) {
		if (crit.userId() != null) {
			query.setLong("userId", crit.userId());
		}

		if (StringUtils.isNotBlank(crit.query())) {
			query.setString("name", "%" + crit.query().toUpperCase() + "%");
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.setParameterList("ids", crit.ids());
		}

		return query;
	}

	private static final String FQN = SpecimenList.class.getName();

	private static final String GET_LIST_CP_SPECIMENS = FQN + ".getListCpSpecimens";

	private static final String GET_LIST_SPMNS_CP_IDS = FQN + ".getListSpecimensCpIds";

	private static final String GET_SPECIMEN_LIST_BY_NAME = FQN + ".getSpecimenListByName";

	private static final String GET_LIST_SPECIMENS_COUNT = FQN + ".getListSpecimensCount";

	private static final String DELETE_LIST_SPECIMENS = FQN + ".deleteListSpecimens";

	private static final String ADD_CHILD_SPECIMENS_MYSQL = FQN + ".addChildSpecimensMySQL";

	private static final String ADD_CHILD_SPECIMENS_ORA = FQN + ".addChildSpecimensOracle";
}
