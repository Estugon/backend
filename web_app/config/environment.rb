# Be sure to restart your server when you modify this file

# Specifies gem version of Rails to use when vendor/rails is not present
RAILS_GEM_VERSION = '2.3.7' unless defined? RAILS_GEM_VERSION
ENV['VM_WATCH_FOLDER'] = File.join(File.dirname(__FILE__), '..', 'tmp', 'vmwatch')

# Bootstrap the Rails environment, frameworks, and default configuration
require File.join(File.dirname(__FILE__), 'boot')

Rails::Initializer.run do |config|
  # Settings in config/environments/* take precedence over those specified here.
  # Application configuration should go into files in config/initializers
  # -- all .rb files in that directory are automatically loaded.

  # Add additional load paths for your own custom dirs
  # config.load_paths += %W( #{RAILS_ROOT}/extras )
  %w(observers sweepers mailers middleware).each do |dir|
    config.load_paths << "#{RAILS_ROOT}/app/#{dir}"
  end

  config.load_paths << "#{RAILS_ROOT}/app/models/events"
  config.load_paths << "#{RAILS_ROOT}/app/models/fake_checks"
  config.load_paths << "#{RAILS_ROOT}/app/models/surveys"
  config.load_paths << "#{RAILS_ROOT}/app/models/quassum"
  config.load_paths << "#{RAILS_ROOT}/app/models/tags"
  config.load_paths << "#{RAILS_ROOT}/app/sweepers"

  # Specify gems that this application depends on and have them installed with rake gems:install
  # config.gem "bj"
  # config.gem "hpricot", :version => '0.6', :source => "http://code.whytheluckystiff.net"
  # config.gem "sqlite3-ruby", :lib => "sqlite3"
  # config.gem "aws-s3", :lib => "aws/s3"

  gem "surveyor"
  gem "rubyzip"
  gem "nokogiri"
  #config.gem "cap-recipes", :lib => false, :source => "http://gemcutter.org" # installed as plugin
  gem "daemons"
  gem "state_machine"
  gem "erubis"
  config.gem "ezprint"
  gem "bluecloth", ">=2.0.0"


  # In later versions of ruby html_safe! for string is not supported...hotfix for thi
  # TODO: Fix this in views when updating to rails 3!
  unless String.respond_to? :html_safe!
    class String
      def html_safe!
       html_safe
      end
    end
  end


  # Setup memcached!
  config.cache_store = :mem_cache_store 

  # Setup Hirb
  begin 
    require 'hirb'
    Hirb.enable 
  rescue LoadError => e
    puts "No Hirb installed, using default"
  end

  def log_to(stream)
    ActiveRecord::Base.logger = Logger.new(stream)
    ActiveRecord::Base.clear_active_connections!
  end

  # Only load the plugins named here, in the order given (default is alphabetical).
  # :all can be used as a placeholder for all plugins not explicitly named
  # config.plugins = [ :exception_notification, :ssl_requirement, :all ]

  # Skip frameworks you're not going to use. To use Rails without a database,
  # you must remove the Active Record framework.
  # config.frameworks -= [ :active_record, :active_resource, :action_mailer ]

  # Activate observers that should always be running
  # config.active_record.observers = :cacher, :garbage_collector, :forum_observer

  # Set Time.zone default to the specified zone and make Active Record auto-convert to this zone.
  # Run "rake -D time" for a list of tasks for finding time zone names.
  config.time_zone = 'UTC'

  # The default locale is :en and all translations from config/locales/*.rb,yml are auto loaded.
  # config.i18n.load_path += Dir[Rails.root.join('my', 'locales', '*.{rb,yml}')]
  config.i18n.load_path += Dir[Rails.root.join('config', 'locales', 'rails', '*.{rb,yml}')]
  config.i18n.load_path += Dir[Rails.root.join('config', 'locales', 'models', '*.{rb,yml}')]
  config.i18n.default_locale = :de

  config.after_initialize do
    require 'core_ext'
    require 'so_cha_manager'
    require 'game_definition'
    require 'season_definitions/season_definition'
    require 'bluecloth'
    require 'state_machine'
    require 'ezprint'

    # Load regular jobs
    #unless $regular_jobs_loaded

      #DailyJob.new.schedule
      #FriendlyEncountersJob.new.schedule

      #$regular_jobs_loaded = true
    #end
  end
end

require 'array_permute'
