package org.dspace.content.dao.hibernate;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.core.Context;

public class CommunityDAOHibernate extends CommunityDAO {
	
    public CommunityDAOHibernate(Context context) {
		super(context);
	}

	/*
	 * Returns all the communities
	 * @see org.dspace.content.dao.CommunityDAO#getCommunities(javax.persistence.EntityManager)
	 */
	public List<Community> getCommunities(EntityManager em) {
		List<Community> communities = null;
		Query q = em.createQuery("SELECT c FROM Community c");
		communities = q.getResultList();
		return communities;
	}
	
	/*
	 * Returns all the top-communities
	 * @see org.dspace.content.dao.CommunityDAO#getTopCommunities(javax.persistence.EntityManager)
	 */
	public List<Community> getTopCommunities(EntityManager em) {
	    List<Community> topcommunities = null;
	    Query q = em.createQuery("SELECT c " +
	    		"FROM Community c " +
	    		"WHERE c IS NOT IN " +
	    		"(SELECT comm FROM Community comm WHERE comm.parentCommunities IS NOT NULL)");
	    topcommunities = q.getResultList();
	    return topcommunities;
	}
	
	
	/*
     * Returns, without calculating it, the number of items owned by the community (retrieves the persisted value)
     * @see org.dspace.content.dao.CommunityDAO#count(org.dspace.content.Community, javax.persistence.EntityManager)
     */
	//TODO testare
	public Integer getCount(Community community, EntityManager em)
    {
	    if(community==null) throw new IllegalArgumentException("Community in itemCount may not be null");
        Query q = em
                .createQuery("SELECT itemCount FROM CommunityItemCount communityItemCount WHERE communityItemCount.community= :community");
        q.setParameter("community", community);
        Integer itemCount = (Integer) q.getSingleResult();
        return itemCount;

    }
	
	/*
     * Calculates and returns the number of items owned by the community (does not read the persisted value but re-calculates it)
     * @see org.dspace.content.dao.CommunityDAO#getCount(org.dspace.content.Community, javax.persistence.EntityManager)
     */
	public Integer count(Community community, EntityManager em)
    {
	    if(community==null) throw new IllegalArgumentException("Community in itemCount may not be null");
	    Query q = em.createQuery("SELECT COUNT(i) " +
	    		                 "FROM Community community, IN (community.collections) AS collections, IN (collections.items) AS i " +
	    		                 "WHERE community = :community AND i.in_archive = true AND i.withdrawn = FALSE");
	    q.setParameter("community", community);
	    Integer itemCount = (Integer) q.getSingleResult();
	    return itemCount;
    }
	
    public Community getCommunityByUUID(UUID uuid, Context context) {
        EntityManager em = context.getEntityManager();
        Query q = em.createQuery("SELECT c FROM Collection c WHERE c.uuid = :uuid");
        q.setParameter("uuid", uuid);
        Community community = (Community)q.getSingleResult();
        return community;
    }

}
