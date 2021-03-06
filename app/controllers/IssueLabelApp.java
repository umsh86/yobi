/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2012 NAVER Corp.
 * http://yobi.io
 *
 * @Author Yi EungJun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package controllers;

import controllers.annotation.IsAllowed;
import controllers.annotation.IsCreatable;
import models.IssueLabel;
import models.Project;
import models.enumeration.Operation;
import models.enumeration.ResourceType;
import play.data.DynamicForm;
import play.data.Form;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.ErrorViews;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static play.data.Form.form;
import static play.libs.Json.toJson;

public class IssueLabelApp extends Controller {
    /**
     * 특정 프로젝트의 모든 이슈라벨을 달라는 요청에 응답한다.
     *
     * when: 이슈에 라벨을 붙일 때, 이슈의 고급검색에서 라벨의 목록을 보여줄 때.
     *
     * 주어진 {@code ownerName}과 {@code projectName}에 대응되는 프로젝트에 대해, 그 프로젝트에 속한 이슈라벨의
     * 리스트를 {@code application/json} 형식으로 인코딩한다. 이 때 인코딩된 리스트에 담겨있는 각 라벨은
     * {@link IssueLabel#id}, {@link IssueLabel#category}, {@link IssueLabel#color},
     * {@link IssueLabel#name} 필드를 갖는다. 이 인코딩된 리스트를 본문으로 하여 응답을 돌려준다.
     *
     * 사용자에게 프로젝트에 접근할 권한이 없는 경우에는 {@code 403 Forbidden}으로 응답한다.
     *
     * 클라이언트가 {@code application/json}을 받아들일 수 없는 경우에는 {@code 406 Not Acceptable}로 응답한다.
     * 성공적인 응답에서 엔터티 본문의 미디어 타입은 언제나 {@code application/json}이기 때문이다.
     *
     * @param ownerName   프로젝트 소유자의 이름
     * @param projectName 프로젝트의 이름
     * @return 이슈라벨들을 달라는 요청에 대한 응답
     */
    @IsAllowed(Operation.READ)
    public static Result labels(String ownerName, String projectName) {
        if (!request().accepts("application/json")) {
            return status(Http.Status.NOT_ACCEPTABLE);
        }

        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);

        List<Map<String, String>> labels = new ArrayList<>();
        for (IssueLabel label : IssueLabel.findByProject(project)) {
            Map<String, String> labelPropertyMap = new HashMap<>();
            labelPropertyMap.put("id", "" + label.id);
            labelPropertyMap.put("category", label.category);
            labelPropertyMap.put("color", label.color);
            labelPropertyMap.put("name", label.name);
            labels.add(labelPropertyMap);
        }

        response().setHeader("Content-Type", "application/json");
        return ok(toJson(labels));
    }

    /**
     * 특정 프로젝트에 새 이슈라벨 하나를 추가해달라는 요청에 응답한다.
     *
     * when: 사용자가 고급검색, 이슈편집, 이슈등록 페이지에서 새 이슈라벨의 추가를 시도했을 때
     *
     * 주어진 {@code ownerName}과 {@code projectName}에 대응되는 프로젝트에,
     * 요청에서 {@link Form#bindFromRequest(java.util.Map, String...)}로 가져온 {@link Form}의 값들에 따라
     * 만들어진 이슈라벨을 추가한다.
     *
     * 그러나 그 프로젝트에 카테고리, 이름, 색상이 모두 같은 이슈라벨이 있다면 추가하지 않고 본문 없이
     * {@code 204 No Content}로 응답한다.
     *
     * 새로 이슈라벨을 추가한 경우, 그 이슈라벨의 {@link IssueLabel#id}, {@link IssueLabel#name},
     * {@link IssueLabel#color}, {@link IssueLabel#category} 필드를 {@code application/json} 형식으로
     * 인코딩한 뒤, 이를 본문으로 하여 {@code 201 Created}로 응답한다. 그러나 이 때 클라이언트가
     * {@code application/json}을 받아들일 수 없다면 본문 없이 응답한다.
     *
     * 사용자에게 프로젝트에 이슈라벨을 만들 권한이 없는 경우에는 {@code 403 Forbidden}으로 응답한다.
     *
     * @param ownerName   프로젝트 소유자의 이름
     * @param projectName 프로젝트의 이름
     * @return 이슈라벨을 추가해달라는 요청에 대한 응답
     */
    @Transactional
    @IsCreatable(ResourceType.ISSUE_LABEL)
    public static Result newLabel(String ownerName, String projectName) {
        Form<IssueLabel> labelForm = new Form<>(IssueLabel.class).bindFromRequest();
        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);

        IssueLabel label = labelForm.get();
        label.project = project;

        if (label.exists()) {
            return noContent();
        } else {
            label.save();

            if (!request().accepts("application/json")) {
                return created();
            }

            response().setHeader("Content-Type", "application/json");

            Map<String, String> labelPropertyMap = new HashMap<>();
            labelPropertyMap.put("id", "" + label.id);
            labelPropertyMap.put("name", label.name);
            labelPropertyMap.put("color", label.color);
            labelPropertyMap.put("category", label.category);

            return created(toJson(labelPropertyMap));
        }
    }

    /**
     * 이슈라벨 하나를 삭제해달라는 요청에 응답한다.
     *
     * when: 사용자가 고급검색, 이슈편집, 이슈등록 페이지에서 이슈라벨의 삭제 버튼을 클릭했을 때
     *
     * 주어진 {@code id}에 대응되는 이슈라벨을 찾아 삭제한다. 대응되는 이슈라벨이 없다면
     * {@code 404 Not Found}로 응답하며, 이슈라벨은 있지만 사용자에게 그것을 삭제할 권한이 없는 경우에는
     * {@code 403 Forbidden}으로 응답한다. 이슈라벨이 성공적으로 삭제되었다면 {@code 200 OK}로 응답한다.
     *
     * 요청에 반드시 {@code _method} 파라메터가 들어있어야 하며, 그 값은 "delete"여야 한다. (대소문자 구분하지
     * 않음) 그렇지 않다면 {@code 400 Bad request}로 응답한다. 이는 DELETE 메소드를 흉내내는 방법이며, 이렇게
     * 하는 이유는 HTML Form이 DELETE 메소드를 지원하지 않기 때문이다.
     *
     * @param ownerName   사용하지 않음
     * @param projectName 사용하지 않음
     * @param id          삭제할 이슈라벨의 아이디
     * @return 이슈라벨을 삭제해달라는 요청에 대한 응답
     */
    @Transactional
    @IsAllowed(value = Operation.DELETE, resourceType = ResourceType.ISSUE_LABEL)
    public static Result delete(String ownerName, String projectName, Long id) {
        // _method must be 'delete'
        DynamicForm bindedForm = form().bindFromRequest();
        if (!bindedForm.get("_method").toLowerCase()
                .equals("delete")) {
            return badRequest(ErrorViews.BadRequest.render("_method must be 'delete'."));
        }

        IssueLabel label = IssueLabel.finder.byId(id);
        label.delete();
        return ok();
    }

    /**
     * 특정 프로젝트의 모든 이슈라벨의 CSS 스타일을 달라는 요청에 응답한다
     *
     * when: 이슈 라벨을 사용하는 페이지에서 라벨 색상(스타일)을 필요로 할 때
     *
     * 주어진 {@code ownerName}과 {@code projectName}에 대응되는 프로젝트에 대해,
     * 그 프로젝트에 속한 이슈라벨의 스타일을 {@code text/css} 형식으로 응답한다.
     *
     * 사용자에게 프로젝트에 접근할 권한이 없는 경우에는 {@code 403 Forbidden}으로 응답한다.
     *
     * @param ownerName   프로젝트 소유자의 이름
     * @param projectName 프로젝트의 이름
     * @return 이슈라벨의 스타일을 달라는 요청에 대한 응답
     */
    @IsAllowed(Operation.READ)
    public static Result labelStyles(String ownerName, String projectName) {
        Project project = Project.findByOwnerAndProjectName(ownerName, projectName);
        List<IssueLabel> labels = IssueLabel.findByProject(project);

        String eTag = "\"" + labels.hashCode() + "\"";
        String ifNoneMatchValue = request().getHeader("If-None-Match");

        if(ifNoneMatchValue != null && ifNoneMatchValue.equals(eTag)) {
            response().setHeader("ETag", eTag);
            return status(NOT_MODIFIED);
        }

        response().setHeader("ETag", eTag);

        return ok(views.html.common.issueLabelColor.render(labels)).as("text/css");
    }
}
