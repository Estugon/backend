class ContestsController < ApplicationController

  access_control do

    allow :administrator

    actions :show, :standings, :results do
      allow all
    end

  end

  # GET /contests
  # GET /contests.xml
  def index
    @contests = Contest.all

    respond_to do |format|
      format.html # index.html.erb
      format.xml  { render :xml => @contests }
    end
  end

  # GET /contests/1
  # GET /contests/1.xml
  def show
    @matchdays = @contest.matchdays

    respond_to do |format|
      format.html
      format.xml  { render :xml => @contest }
    end
  end

  def standings
    @matchday = @contest.last_played_matchday

    redirect_to @contest unless @matchday
  end

  def results
    @matchday = @contest.last_played_matchday

    redirect_to @contest unless @matchday
  end

  # GET /contests/new
  # GET /contests/new.xml
  def new
    @contest = Contest.new

    respond_to do |format|
      format.html # new.html.erb
      format.xml  { render :xml => @contest }
    end
  end

  # GET /contests/1/edit
  def edit
    
  end

  # POST /contests
  # POST /contests.xml
  def create
    @contest = Contest.new(params[:contest])
    puts @contest.attributes["game_definition"]

    respond_to do |format|
      if @contest.save
        flash[:notice] = I18n.t("messages.contest_created_successfully")
        format.html { redirect_to contests_url }
        format.xml  { render :xml => @contest, :status => :created, :location => contests_url }
      else
        format.html { render :action => "new" }
        format.xml  { render :xml => @contest.errors, :status => :unprocessable_entity }
      end
    end
  end

  # PUT /contests/1
  # PUT /contests/1.xml
  def update
    respond_to do |format|
      if @contest.update_attributes(params[:contest])
        flash[:notice] = I18n.t("messages.contest.updated_successfully")
        format.html { redirect_to admin_contests_url }
        format.xml  { head :ok, :location => admin_contests_url }
      else
        @test_contestant = @contest.test_contestant
        format.html { render :action => "edit" }
        format.xml  { render :xml => @contest.errors, :status => :unprocessable_entity }
      end
    end
  end

  # DELETE /contests/1
  # DELETE /contests/1.xml
  def destroy
    raise "not supported"
    
    @contest.destroy

    respond_to do |format|
      format.html { redirect_to(contests_url) }
      format.xml  { head :ok }
    end
  end

  def edit_schedule

  end

  def reset_matchdays
    @contest.matchdays.destroy_all

    redirect_to contest_edit_schedule_url
  end

  def refresh_matchdays
    start_at_param = read_multipart_param(params[:schedule], :start_at)
    start_at = Date.new(*start_at_param.collect{ |x| x.to_i })
    weekdays = params[:schedule][:weekdays].collect { |x| x.blank? ? nil : x.to_i }
    weekdays.compact!
    weekdays.uniq!

    if @contest.matchdays.count.zero?
      @contest.refresh_matchdays!(start_at, weekdays)

      if @contest.matchdays.count.zero?
        flash[:error] = I18n.t("messages.not_enough_contestants_for_creating_contest", :contestant_label => Contestant.human_name(:count => 2))
      end
    else
      flash[:error] = I18n.t("messages.there_is_already_a_schedule")
    end

    redirect_to contest_edit_schedule_url
  end


  def reaggregate
    if not @current_contest.reaggregate
      flash[:error] = I18n.t("messages.matchday_playing")
    end
    
    redirect_to contest_standings_url()
  end

  protected

  def read_multipart_param(data, key, count = 3)
    (1..count).collect do |i|
      data["#{key}(#{i}i)"]
    end
  end
end
