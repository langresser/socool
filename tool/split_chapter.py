import re,os,struct,glob;

g_config_juan = {};
g_config_zhang = {};

def load_config():
    fp = open('split.cfg', 'r');
    current_section = "";
    current_data = [];
    for line in fp:
        if re.search('^\[.+\]$', line):
            current_section = line[line.find('[') + 1 : line.find(']')];
            if current_section not in g_config_juan:
                g_config_juan[current_section] = [];
            if current_section not in g_config_zhang:
                g_config_zhang[current_section] = [];
            continue;

        if re.search('卷=', line):
            data = line[line.find('=') + 1:].strip();
            g_config_juan[current_section].append(data);
            continue;

        if re.search('章=', line):
            data = line[line.find('=') + 1:].strip();
            g_config_zhang[current_section].append(data);
            continue;



def is_juan_title(line, file):
    if len(line) > 30:
        return False;
    data = g_config_juan['公共'];
    for each in data:
        if re.search(each, line):
            return True;

    if file in g_config_juan:
        data_ex = g_config_juan[file];
        for each in data_ex:
            if re.search(each, line):
                return True;

    return False;
def is_chapter_title(line, file):
    if len(line) > 30:
        return False;
    data = g_config_zhang['公共'];
    for each in data:
        if re.search(each, line):
            return True;

    if file in g_config_zhang:
        data_ex = g_config_zhang[file];
        for each in data_ex:
            if re.search(each, line):
                return True;

    return False;

def get_juan_chapter(each):
    print(each[0])
    title_line = each[0].strip().split('@@');

    juan = title_line[0];
    zhang = title_line[1];
    return juan,zhang
def split_file(file):
    print(file);
    file_name = os.path.basename(file);
    file_name = file_name[:file_name.rfind('.')];
    fp = open(file, 'r');
    dir_name = file[:file.rfind('.')];

    if not os.path.exists(dir_name):
        os.makedirs(dir_name);

    all_text = [];
    data_text = [];

    current_juan = "";
    for line in fp:
        if line == '\n':
            continue;

        if is_juan_title(line, file_name):
            current_juan = line.strip();
            continue;

        if is_chapter_title(line, file_name):
            if len(data_text) > 0:
                all_text.append(data_text);

            data_text = [];
            print(current_juan + '@@' + line)
            data_text.append(current_juan + '@@' + line);
            continue;

        data_text.append(line);
    fp.close();
    if len(data_text) > 0:
        all_text.append(data_text);

    fpwd = open('{0}/chapter.txt'.format(dir_name), 'w');
    fpwdata = open('{0}/data_read.txt'.format(dir_name), 'w');


    i = -1;
    current_juan = "";
    paragra_data = [];

    for each in all_text:
        i += 1;
        juan,zhang = get_juan_chapter(each)
        
        each[0] = zhang + '\n';
  
        if each[len(each) - 1] == '\n':
            each.pop();
        size = 0;
        paragraph = 0;
        paragra_data.append(0);
        fpwdata.write('c{0}  {1}\n'.format(i, zhang));
        for eee in each:
            textLen = len(eee);
            fpwdata.write('{0}\n'.format(textLen));

            if textLen >=512:
                print(eee);

            if textLen >= 255:
                paragra_data.append(255);
                if textLen - 255 >= 255:
                    paragra_data.append(255);
                    paragra_data.append(textLen - 255 - 255);
                else:
                    paragra_data.append(textLen - 255);
            else:
                paragra_data.append(textLen);
            
            size += textLen;
            paragraph += 1;

        if juan != current_juan:
            current_juan = juan;
            fpwd.write('j{0}\n'.format(juan));
            fpwd.write('{0}@@{1}@@{2}\n'.format(len(each), size, zhang));
        else:
            fpwd.write('{0}@@{1}@@{2}\n'.format(len(each), size, zhang));

        fpw = open('{0}/{1}.txt'.format(dir_name, i), 'w');
        fpw.writelines(each);
        fpw.close();

    fpwd.close();
    fpwdata.close();

    fpws = open('{0}/data.db'.format(dir_name), 'wb');
    for each in paragra_data:
        bytes = struct.pack('B', each);
        fpws.write(bytes);
    fpws.close();


load_config();
print(g_config_juan);
file_list = glob.glob('*.TXT')
for file in file_list:
    split_file(file);
