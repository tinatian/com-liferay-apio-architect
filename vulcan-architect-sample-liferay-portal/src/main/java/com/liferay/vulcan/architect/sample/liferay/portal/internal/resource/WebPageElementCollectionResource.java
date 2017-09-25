/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.vulcan.architect.sample.liferay.portal.internal.resource;

import com.liferay.document.library.kernel.service.DLFolderService;
import com.liferay.journal.exception.NoSuchArticleException;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.portal.kernel.exception.NoSuchGroupException;
import com.liferay.portal.kernel.exception.NoSuchUserException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.service.GroupService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserService;
import com.liferay.portal.kernel.util.DateUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.vulcan.architect.pagination.PageItems;
import com.liferay.vulcan.architect.pagination.Pagination;
import com.liferay.vulcan.architect.resource.CollectionResource;
import com.liferay.vulcan.architect.resource.Representor;
import com.liferay.vulcan.architect.resource.Routes;
import com.liferay.vulcan.architect.resource.builder.RepresentorBuilder;
import com.liferay.vulcan.architect.resource.builder.RoutesBuilder;
import com.liferay.vulcan.architect.resource.identifier.LongIdentifier;
import com.liferay.vulcan.architect.result.Try;

import java.text.DateFormat;
import java.text.ParseException;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServerErrorException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides all the necessary information to expose <a
 * href="http://schema.org/WebPageElement">WebPageElement</a> resources through
 * a web API. <p> The resources are mapped from the internal {@link
 * JournalArticle} model.
 *
 * @author Javier Gamarra
 * @review
 */
@Component(immediate = true)
public class WebPageElementCollectionResource
	implements CollectionResource<JournalArticle, LongIdentifier> {

	@Override
	public Representor<JournalArticle, LongIdentifier> buildRepresentor(
		RepresentorBuilder<JournalArticle, LongIdentifier> representorBuilder) {

		Function<Date, String> formatFunction = date -> {
			if (date == null) {
				return null;
			}

			DateFormat dateFormat = DateUtil.getISO8601Format();

			return dateFormat.format(date);
		};

		return representorBuilder.identifier(
			journalArticle -> journalArticle::getId
		).addBidirectionalModel(
			"group", "blogs", Group.class, this::_getGroupOptional,
			group -> (LongIdentifier)group::getGroupId
		).addEmbeddedModel(
			"creator", User.class, this::_getUserOptional
		).addLinkedModel(
			"author", User.class, this::_getUserOptional
		).addStringField(
			"dateCreated",
			journalArticle -> formatFunction.apply(
				journalArticle.getCreateDate())
		).addStringField(
			"dateModified",
			journalArticle -> formatFunction.apply(
				journalArticle.getModifiedDate())
		).addStringField(
			"datePublished",
			journalArticle -> formatFunction.apply(
				journalArticle.getLastPublishDate())
		).addStringField(
			"description", JournalArticle::getDescription
		).addStringField(
			"lastReviewed",
			journalArticle -> formatFunction.apply(
				journalArticle.getReviewDate())
		).addStringField(
			"text", JournalArticle::getContent
		).addStringField(
			"title", JournalArticle::getTitle
		).addType(
			"WebPageElement"
		).build();
	}

	@Override
	public String getName() {
		return "web-page-elements";
	}

	@Override
	public Routes<JournalArticle> routes(
		RoutesBuilder<JournalArticle, LongIdentifier> routesBuilder) {

		return routesBuilder.addCollectionPageGetter(
			this::_getPageItems, LongIdentifier.class
		).addCollectionPageItemCreator(
			this::_addJournalArticle, LongIdentifier.class
		).addCollectionPageItemGetter(
			this::_getJournalArticle
		).addCollectionPageItemRemover(
			this::_deleteJournalArticle
		).addCollectionPageItemUpdater(
			this::_updateJournalArticle
		).build();
	}

	private JournalArticle _addJournalArticle(
		LongIdentifier groupIdLongIdentifier, Map<String, Object> body) {

		String folderIdString = (String)body.get("folder");
		String title = (String)body.get("title");
		String description = (String)body.get("description");
		String content = (String)body.get("text");
		String ddmStructureKey = (String)body.get("structure");
		String ddmTemplateKey = (String)body.get("template");
		String displayDateString = (String)body.get("dateDisplayed");

		Supplier<BadRequestException> incorrectBodyExceptionSupplier =
			() -> new BadRequestException("Invalid body");

		if (Validator.isNull(folderIdString) || Validator.isNull(title) ||
			Validator.isNull(description) || Validator.isNull(content) ||
			Validator.isNull(ddmStructureKey) ||
			Validator.isNull(ddmTemplateKey) ||
			Validator.isNull(displayDateString)) {

			throw incorrectBodyExceptionSupplier.get();
		}

		Try<Long> folderIdLongTry = Try.fromFallible(
			() -> Long.valueOf(folderIdString));

		long folderId = folderIdLongTry.orElse(0L);

		Map<Locale, String> titleMap = new HashMap<>();

		titleMap.put(Locale.getDefault(), title);

		Map<Locale, String> descriptionMap = new HashMap<>();

		descriptionMap.put(Locale.getDefault(), description);

		Calendar calendar = Calendar.getInstance();

		Try<DateFormat> dateFormatTry = Try.success(
			DateUtil.getISO8601Format());

		Date displayDate = dateFormatTry.map(
			dateFormat -> dateFormat.parse(displayDateString)
		).mapFailMatching(
			ParseException.class, incorrectBodyExceptionSupplier
		).getUnchecked();

		calendar.setTime(displayDate);

		int displayDateMonth = calendar.get(Calendar.MONTH);
		int displayDateDay = calendar.get(Calendar.DATE);
		int displayDateYear = calendar.get(Calendar.YEAR);
		int displayDateHour = calendar.get(Calendar.HOUR);
		int displayDateMinute = calendar.get(Calendar.MINUTE);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupIdLongIdentifier.getId());

		Try<JournalArticle> journalArticleTry = Try.fromFallible(() ->
			_journalArticleService.addArticle(
				groupIdLongIdentifier.getId(), folderId, 0, 0, null, true,
				titleMap, descriptionMap, content, ddmStructureKey,
				ddmTemplateKey, null, displayDateMonth, displayDateDay,
				displayDateYear, displayDateHour, displayDateMinute, 0, 0, 0, 0,
				0, true, 0, 0, 0, 0, 0, true, true, null, serviceContext));

		return journalArticleTry.getUnchecked();
	}

	private void _deleteJournalArticle(
		LongIdentifier journalArticleIdLongIdentifier) {

		try {
			JournalArticle article = _journalArticleService.getArticle(
				journalArticleIdLongIdentifier.getId());

			_journalArticleService.deleteArticle(
				article.getGroupId(), article.getArticleId(),
				article.getArticleResourceUuid(), new ServiceContext());
		}
		catch (NoSuchArticleException nsae) {
		}
		catch (PortalException pe) {
			throw new ServerErrorException(500, pe);
		}
	}

	private Optional<Group> _getGroupOptional(JournalArticle journalArticle) {
		try {
			return Optional.of(
				_groupService.getGroup(journalArticle.getGroupId()));
		}
		catch (NoSuchGroupException nsge) {
			throw new NotFoundException(
				"Unable to get group " + journalArticle.getGroupId(), nsge);
		}
		catch (PortalException pe) {
			throw new ServerErrorException(500, pe);
		}
	}

	private JournalArticle _getJournalArticle(
		LongIdentifier journalArticleIdLongIdentifier) {

		try {
			return _journalArticleService.getArticle(
				journalArticleIdLongIdentifier.getId());
		}
		catch (NoSuchArticleException nsae) {
			throw new NotFoundException(
				"Unable to get article " +
					journalArticleIdLongIdentifier.getId(),
				nsae);
		}
		catch (PortalException pe) {
			throw new ServerErrorException(500, pe);
		}
	}

	private PageItems<JournalArticle> _getPageItems(
		Pagination pagination, LongIdentifier groupIdLongIdentifier) {

		List<JournalArticle> journalArticles =
			_journalArticleService.getArticles(
				groupIdLongIdentifier.getId(), 0, pagination.getStartPosition(),
				pagination.getEndPosition(), null);
		int count = _journalArticleService.getArticlesCount(
			groupIdLongIdentifier.getId(), 0);

		return new PageItems<>(journalArticles, count);
	}

	private Optional<User> _getUserOptional(JournalArticle journalArticle) {
		try {
			return Optional.ofNullable(
				_userService.getUserById(journalArticle.getUserId()));
		}
		catch (NoSuchUserException | PrincipalException e) {
			throw new NotFoundException(
				"Unable to get user " + journalArticle.getUserId(), e);
		}
		catch (PortalException pe) {
			throw new ServerErrorException(500, pe);
		}
	}

	private JournalArticle _updateJournalArticle(
		LongIdentifier journalArticleIdLongIdentifier,
		Map<String, Object> body) {

		Double userId = (Double)body.get("user");
		Double groupId = (Double)body.get("group");
		String folderIdString = (String)body.get("folder");
		Double version = (Double)body.get("version");
		String title = (String)body.get("title");
		String description = (String)body.get("description");
		String content = (String)body.get("text");

		Supplier<BadRequestException> incorrectBodyExceptionSupplier =
			() -> new BadRequestException("Invalid body");

		if (Validator.isNull(userId) || Validator.isNull(groupId) ||
			Validator.isNull(folderIdString) || Validator.isNull(version) ||
			Validator.isNull(title) || Validator.isNull(description) ||
			Validator.isNull(content)) {

			throw incorrectBodyExceptionSupplier.get();
		}

		Try<Long> folderIdLongTry = Try.fromFallible(
			() -> Long.valueOf(folderIdString));

		long folderId = folderIdLongTry.orElse(0L);

		Map<Locale, String> titleMap = new HashMap<>();

		titleMap.put(Locale.getDefault(), title);

		Map<Locale, String> descriptionMap = new HashMap<>();

		descriptionMap.put(Locale.getDefault(), description);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(groupId.longValue());

		Try<JournalArticle> journalArticleTry = Try.fromFallible(() ->
			_journalArticleService.updateArticle(
				userId.longValue(), groupId.longValue(), folderId,
				String.valueOf(journalArticleIdLongIdentifier.getId()), version,
				titleMap, descriptionMap, content, null, serviceContext));

		return journalArticleTry.getUnchecked();
	}

	@Reference
	private DLFolderService _dlFolderService;

	@Reference
	private GroupService _groupService;

	@Reference
	private JournalArticleService _journalArticleService;

	@Reference
	private UserService _userService;

}