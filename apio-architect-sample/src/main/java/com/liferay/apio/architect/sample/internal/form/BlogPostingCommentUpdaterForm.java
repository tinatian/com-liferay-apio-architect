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

package com.liferay.apio.architect.sample.internal.form;

import com.liferay.apio.architect.form.Form;
import com.liferay.apio.architect.form.Form.Builder;

/**
 * Instances of this class represent the values extracted from a blog posting
 * comment updater form.
 *
 * @author Alejandro Hernández
 * @review
 */
public class BlogPostingCommentUpdaterForm {

	/**
	 * Builds a {@code Form} that generates {@code
	 * BlogPostingCommentUpdaterForm} depending on the HTTP body.
	 *
	 * @param  formBuilder the {@code Form} builder
	 * @return a blog posting comment updater form
	 * @review
	 */
	public static Form<BlogPostingCommentUpdaterForm> buildForm(
		Builder<BlogPostingCommentUpdaterForm> formBuilder) {

		return formBuilder.constructor(
			BlogPostingCommentUpdaterForm::new
		).addRequiredString(
			"text", BlogPostingCommentUpdaterForm::_setText
		).build();
	}

	/**
	 * Returns the blog posting comment's text
	 *
	 * @return the blog posting comment's text
	 * @review
	 */
	public String getText() {
		return _text;
	}

	private void _setText(String text) {
		_text = text;
	}

	private String _text;

}